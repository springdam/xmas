package com.xmas.game.engine.specialweapon;

import com.xmas.game.actions.specialweapon.SpecialWeaponResolver;
import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.common.error.BadRequestException;
import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.config.SpecialWeaponProperties;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.economy.BuildingDamageService;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.entity.PlayerSpecialWeaponsEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerSpecialWeaponsRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.xmas.game.engine.palace.PalaceService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaSpecialWeaponResolver implements SpecialWeaponResolver {

    private final PlayerRepository playerRepository;
    private final PlayerSpecialWeaponsRepository specialWeaponsRepository;
    private final PlayerMilitaryRepository militaryRepository;
    private final PlayerEconomyRepository economyRepository;
    private final GameRepository gameRepository;
    private final RandomSource random;
    private final SpecialWeaponProperties properties;
    private final PopularityProperties popularityProperties;
    private final ResearchModifierService researchModifierService;
    private final NewsService newsService;
    private final BuildingDamageService buildingDamageService;
    private final PalaceService palaceService;

    @Override
    @Transactional
    public void launch(UUID actorId, UUID targetId, int warheads) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        PlayerSpecialWeaponsEntity arsenal = special(actorId);
        if (arsenal.getNukes() < warheads) {
            throw new BadRequestException("Not enough warheads to launch");
        }
        PlayerMilitaryEntity targetMilitary = military(targetId);
        PlayerEconomyEntity targetEconomy = economy(targetId);

        double interception = Math.min(properties.getMaxInterceptionProbability(),
                targetMilitary.getAirDefenseBatteries() * properties.getInterceptionPerBattery());

        int effectiveWarheads = warheads * researchModifierService.warheadMultiplier(actorId);
        int landed = 0;
        for (int i = 0; i < effectiveWarheads; i++) {
            if (random.nextDouble() >= interception) {
                landed++;
            }
        }

        double destruction = Math.min(1.0, landed * properties.getDestructionPerWarhead());
        applyDestruction(targetEconomy, targetMilitary, destruction);
        arsenal.setNukes(arsenal.getNukes() - warheads);

        specialWeaponsRepository.save(arsenal);
        economyRepository.save(targetEconomy);
        militaryRepository.save(targetMilitary);
        applyStrikePopularity(actor.getGameId(), actorId, targetId);
        if (landed > 0) {
            palaceService.onNuked(targetId, landed, actor.getNation());
        }

        int intercepted = effectiveWarheads - landed;
        newsService.record(actor.getGameId(), NewsType.ATTACK, NewsVisibility.PUBLIC, actorId, targetId,
                "%s launched %d warhead(s) (%d incoming) at %s: %d intercepted, %d struck home.".formatted(
                        actor.getNation(), warheads, effectiveWarheads, target.getNation(), intercepted, landed),
                gameTime(actor));
        log.info("Special weapon strike {} -> {}: {} of {} landed", actorId, targetId, landed, warheads);
    }

    private void applyStrikePopularity(java.util.UUID gameId, java.util.UUID attackerId, java.util.UUID defenderId) {
        for (PlayerEntity player : playerRepository.findByGameId(gameId)) {
            if (player.isAdmin() || player.isEliminated()) {
                continue;
            }
            int drop = popularityProperties.getNukeGlobalDrop();
            if (player.getId().equals(attackerId)) {
                drop += popularityProperties.getNukeAttackerPenalty();
            }
            if (player.getId().equals(defenderId)) {
                drop += popularityProperties.getNukeVictimPenalty();
            }
            int effectiveDrop = (int) Math.round(drop * researchModifierService.popularityDamageMultiplier(player.getId()));
            player.setPopularity(com.xmas.game.engine.domain.Popularity.clamp(player.getPopularity() - effectiveDrop));
            playerRepository.save(player);
        }
    }

    /** Buildings roll against the blast by fragility; ground forces lose a straight fraction. */
    private void applyDestruction(PlayerEconomyEntity economy, PlayerMilitaryEntity military, double fraction) {
        if (fraction > 0) {
            buildingDamageService.strike(economy, fraction);
        }
        military.setInfantry(reduce(military.getInfantry(), fraction));
        military.setTanks(reduce(military.getTanks(), fraction));
    }

    private int reduce(int units, double fraction) {
        return units - Math.min(units, (int) Math.round(units * fraction));
    }

    private Instant gameTime(PlayerEntity player) {
        return gameRepository.findById(player.getGameId())
                .map(GameEntity::getGameTime)
                .orElse(Instant.now());
    }

    private PlayerEntity player(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
    }

    private PlayerSpecialWeaponsEntity special(UUID playerId) {
        return specialWeaponsRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player special weapons not found: " + playerId));
    }

    private PlayerMilitaryEntity military(UUID playerId) {
        return militaryRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player military not found: " + playerId));
    }

    private PlayerEconomyEntity economy(UUID playerId) {
        return economyRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player economy not found: " + playerId));
    }
}
