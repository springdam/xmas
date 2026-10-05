package com.xmas.game.engine.service;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.service.ActionDefinitionCatalog;
import com.xmas.game.actions.service.ActionHandlerRegistry;
import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.domain.GameStatus;
import com.xmas.game.engine.domain.PendingActionStatus;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PendingActionEntity;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PendingActionRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerRelationRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameTickService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final PlayerEconomyRepository economyRepository;
    private final PlayerRelationRepository relationRepository;
    private final PendingActionRepository pendingActionRepository;
    private final EconomyProperties economyProperties;
    private final ActionHandlerRegistry handlerRegistry;
    private final ActionDefinitionCatalog definitionCatalog;
    private final ResearchModifierService researchModifierService;
    private final PopularityProperties popularityProperties;
    private final RandomSource random;
    private final NewsService newsService;

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
            if (player.getWarStrain() > 0
                    && game.getCurrentTick() % popularityProperties.getWarStrainRecoveryTicks() == 0) {
                player.setWarStrain(player.getWarStrain() - 1);
            }
            PlayerEconomyEntity economy = economyRepository.findById(player.getId()).orElse(null);
            if (economy == null) {
                continue;
            }
            player.setCash(player.getCash() + income(player.getId(), economy, player.getPopularity()));
            playerRepository.save(player);
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

    private double popularityIncomeFactor(int popularity) {
        double factor = 1.0 + (popularity - 50) * popularityProperties.getIncomePerPopularityPoint();
        return Math.max(popularityProperties.getMinIncomeFactor(),
                Math.min(popularityProperties.getMaxIncomeFactor(), factor));
    }

    private long income(UUID playerId, PlayerEconomyEntity e, int popularity) {
        double farmIncome = (double) e.getFarms() * economyProperties.getFarmYield();
        double factoryIncome = ((double) e.getFactories() * economyProperties.getFactoryYield()
                + (double) e.getRobotFactories() * economyProperties.getRobotFactoryYield())
                * researchModifierService.factoryIncomeMultiplier(playerId);
        double bankIncome = (double) e.getBanks() * economyProperties.getBankYield()
                * researchModifierService.bankIncomeMultiplier(playerId);
        double raw = economyProperties.getBaseIncome() + farmIncome + factoryIncome + bankIncome;
        double total = raw
                * researchModifierService.economyMultiplier(playerId)
                * popularityIncomeFactor(popularity)
                * researchModifierService.warEconomyMultiplier(playerId, relationRepository.isAtWar(playerId));
        return Math.round(total);
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
