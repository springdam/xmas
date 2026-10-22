package com.xmas.game.engine.service;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.service.ActionDefinitionCatalog;
import com.xmas.game.actions.service.ActionHandlerRegistry;
import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.domain.GameStatus;
import com.xmas.game.engine.domain.PendingActionStatus;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PendingActionEntity;
import com.xmas.game.engine.economy.IncomeService;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.military.UpkeepService;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PendingActionRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xmas.game.engine.palace.PalaceService;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameTickService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final PlayerEconomyRepository economyRepository;
    private final PendingActionRepository pendingActionRepository;
    private final IncomeService incomeService;
    private final ActionHandlerRegistry handlerRegistry;
    private final ActionDefinitionCatalog definitionCatalog;
    private final ResearchModifierService researchModifierService;
    private final PopularityProperties popularityProperties;
    private final RandomSource random;
    private final NewsService newsService;
    private final PlayerMilitaryRepository militaryRepository;
    private final UpkeepService upkeepService;
    private final PalaceService palaceService;

    @Transactional
    public void tick() {
        List<GameEntity> running = gameRepository.findByStatus(GameStatus.RUNNING);
        if (running.isEmpty()) {
            return;
        }
        for (GameEntity game : running) {
            advance(game);
            processPlayers(game);
        }
        resolveDueActions();
    }

    private void advance(GameEntity game) {
        game.setCurrentTick(game.getCurrentTick() + 1);
        game.setGameTime(game.getGameTime().plusMillis(game.getTickDurationMillis()));
        gameRepository.save(game);
    }

    private void processPlayers(GameEntity game) {
        for (PlayerEntity player : playerRepository.findByGameId(game.getId())) {
            if (player.isAdmin() || player.isEliminated()) {
                continue;
            }
            if (player.getPopularity() < popularityProperties.getVulnerabilityThreshold()
                    && random.nextDouble() < popularityProperties.getCoupChancePerTick()
                            * researchModifierService.coupChanceMultiplier(player.getId())) {
                overthrow(game, player);
                continue;
            }
            palaceService.onTick(game, player);
            if (player.getPopularityBoostCount() > 0
                    && game.getCurrentTick() % popularityProperties.getBoostRecoveryTicks() == 0) {
                player.setPopularityBoostCount(player.getPopularityBoostCount() - 1);
            }
            if (player.getWarStrain() > 0
                    && game.getCurrentTick() % popularityProperties.getWarStrainRecoveryTicks() == 0) {
                player.setWarStrain(player.getWarStrain() - 1);
            }
            PlayerEconomyEntity economy = economyRepository.findById(player.getId()).orElse(null);
            if (economy == null) {
                continue;
            }
            player.setCash(player.getCash() + incomeService.perTick(player.getId(), economy, player.getPopularity()));
            payUpkeep(game, player);
            playerRepository.save(player);
        }
    }

    /** Pays the army from the treasury; if the money runs out, cash stops at zero and troops desert. */
    private void payUpkeep(GameEntity game, PlayerEntity player) {
        PlayerMilitaryEntity military = militaryRepository.findById(player.getId()).orElse(null);
        if (military == null) {
            return;
        }
        long upkeep = upkeepService.perTick(military);
        if (player.getCash() >= upkeep) {
            player.setCash(player.getCash() - upkeep);
            return;
        }
        player.setCash(0);
        UpkeepService.Deserters deserters = upkeepService.desert(military);
        militaryRepository.save(military);
        if (deserters.any()) {
            newsService.record(game.getId(), NewsType.ATTACK, NewsVisibility.ACTOR_ONLY, player.getId(), null,
                    "Unpaid troops deserted: %d infantry, %d tanks, %d bombers and %d special ops walked off."
                            .formatted(deserters.getInfantry(), deserters.getTanks(),
                                    deserters.getStealthBombers(), deserters.getSpecialOps()),
                    game.getGameTime());
        }
    }

    private void overthrow(GameEntity game, PlayerEntity player) {
        player.setEliminated(true);
        playerRepository.save(player);
        newsService.record(game.getId(), NewsType.DIPLOMACY, NewsVisibility.PUBLIC, player.getId(), null,
                "%s's government was overthrown in a coup after collapsing popular support.".formatted(
                        player.getNation()),
                game.getGameTime());
        log.info("Player {} ({}) overthrown by coup at popularity {}",
                player.getId(), player.getNation(), player.getPopularity());
    }

    private void resolveDueActions() {
        List<PendingActionEntity> due = pendingActionRepository
                .findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(PendingActionStatus.PENDING, Instant.now());
        for (PendingActionEntity pending : due) {
            try {
                complete(pending);
                pending.setStatus(PendingActionStatus.COMPLETED);
            } catch (RuntimeException ex) {
                pending.setStatus(PendingActionStatus.CANCELLED);
                log.warn("Pending action {} ({}) failed to resolve: {}",
                        pending.getId(), pending.getActionType(), ex.getMessage());
            }
            pendingActionRepository.save(pending);
        }
    }

    private void complete(PendingActionEntity pending) {
        ActionType type = pending.getActionType();
        ActionHandler handler = handlerRegistry.get(type);
        ActionContext ctx = ActionContext.builder()
                .actorPlayerId(pending.getActorPlayerId())
                .targetPlayerId(pending.getTargetPlayerId())
                .quantity(pending.getQuantity())
                .definition(definitionCatalog.getDefinition(type))
                .build();
        handler.onComplete(ctx);
    }
}
