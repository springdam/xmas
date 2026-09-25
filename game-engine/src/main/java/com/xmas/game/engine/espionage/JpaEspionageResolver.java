package com.xmas.game.engine.espionage;

import com.xmas.game.actions.espionage.EspionageResolver;
import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.config.EspionageProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.config.SpecialForcesProperties;
import com.xmas.game.engine.domain.Popularity;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerEspionageEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.entity.PlayerResearchEntity;
import com.xmas.game.engine.protection.LeaderKillService;
import com.xmas.game.engine.protection.ProtectionService;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerEspionageRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerResearchRepository;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaEspionageResolver implements EspionageResolver {

    private final PlayerRepository playerRepository;
    private final PlayerEspionageRepository espionageRepository;
    private final PlayerEconomyRepository economyRepository;
    private final PlayerMilitaryRepository militaryRepository;
    private final PlayerResearchRepository researchRepository;
    private final GameRepository gameRepository;
    private final RandomSource random;
    private final EspionageProperties properties;
    private final PopularityProperties popularityProperties;
    private final ResearchModifierService researchModifierService;
    private final ResearchGateway researchGateway;
    private final ProtectionService protectionService;
    private final LeaderKillService leaderKillService;
    private final SpecialForcesProperties specialForcesProperties;
    private final NewsService newsService;

    @Override
    @Transactional
    public void performHeist(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        if (!attemptSucceeds(actorId, targetId)) {
            recordFailure(actor, target, "heist");
            return;
        }
        long stolen = Math.round(target.getCash() * properties.getHeistCashFraction());
        target.setCash(target.getCash() - stolen);
        target.setPopularity(Popularity.clamp(target.getPopularity() - victimPenalty(targetId, popularityProperties.getHeistVictimPenalty())));
        actor.setCash(actor.getCash() + stolen);
        playerRepository.save(actor);
        playerRepository.save(target);
        record(actor, target, "%s pulled off a heist against %s, stealing $%d.".formatted(
                actor.getNation(), target.getNation(), stolen));
    }

    @Override
    @Transactional
    public void performSabotage(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        if (!attemptSucceeds(actorId, targetId)) {
            recordFailure(actor, target, "sabotage");
            return;
        }
        PlayerMilitaryEntity military = military(targetId);
        int infantryLost = scaled(military.getInfantry(), properties.getSabotageFraction());
        int tanksLost = scaled(military.getTanks(), properties.getSabotageFraction());
        military.setInfantry(military.getInfantry() - infantryLost);
        military.setTanks(military.getTanks() - tanksLost);
        militaryRepository.save(military);
        target.setPopularity(Popularity.clamp(target.getPopularity() - victimPenalty(targetId, popularityProperties.getSabotageVictimPenalty())));
        playerRepository.save(target);
        record(actor, target, "%s sabotaged %s's forces, destroying %d infantry and %d tanks.".formatted(
                actor.getNation(), target.getNation(), infantryLost, tanksLost));
    }

    @Override
    @Transactional
    public void performCyberAttack(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        if (!attemptSucceeds(actorId, targetId)) {
            recordFailure(actor, target, "cyber attack");
            return;
        }
        PlayerEconomyEntity economy = economy(targetId);
        int factoriesLost = scaled(economy.getFactories(), properties.getCyberFraction());
        int robotFactoriesLost = scaled(economy.getRobotFactories(), properties.getCyberFraction());
        economy.setFactories(economy.getFactories() - factoriesLost);
        economy.setRobotFactories(economy.getRobotFactories() - robotFactoriesLost);
        economyRepository.save(economy);
        target.setPopularity(Popularity.clamp(target.getPopularity() - victimPenalty(targetId, popularityProperties.getCyberVictimPenalty())));
        playerRepository.save(target);
        record(actor, target, "%s's cyber attack crippled %s's industry, disabling %d factories and %d robot factories."
                .formatted(actor.getNation(), target.getNation(), factoriesLost, robotFactoriesLost));
    }

    @Override
    @Transactional
    public void stealTechnology(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        if (!attemptSucceeds(actorId, targetId)) {
            recordFailure(actor, target, "technology theft");
            return;
        }
        ActionType stolen = null;
        for (PlayerResearchEntity researched : researchRepository.findByPlayerId(targetId)) {
            if (!researchGateway.hasResearch(actorId, researched.getResearchType())) {
                stolen = researched.getResearchType();
                break;
            }
        }
        if (stolen == null) {
            record(actor, target, "%s infiltrated %s but found no new technology to steal.".formatted(
                    actor.getNation(), target.getNation()));
            return;
        }
        researchGateway.addResearch(actorId, stolen);
        record(actor, target, "%s stole %s from %s.".formatted(actor.getNation(), stolen, target.getNation()));
    }

    @Override
    @Transactional
    public void gatherIntelligence(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        PlayerEspionageEntity actorEsp = espionage(actorId);

        double spies = actorEsp.getSpies() * researchModifierService.espionageMultiplier(actorId);
        double counterIntel = espionage(targetId).getCounterIntel()
                * researchModifierService.counterIntelMultiplier(targetId);
        double clarity = spies <= 0 ? 0.0 : spies / (spies + counterIntel + properties.getBaseResistance());
        boolean success = random.nextDouble() < clarity;

        int spiesLost = Math.min(actorEsp.getSpies(),
                (int) Math.round(counterIntel * properties.getReconSpyLossPerCounterIntel()));
        if (spiesLost > 0) {
            actorEsp.setSpies(actorEsp.getSpies() - spiesLost);
            espionageRepository.save(actorEsp);
            newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.INVOLVED_ONLY, actorId, targetId,
                    "%s's counter-intelligence caught and expelled %d foreign spies.".formatted(
                            target.getNation(), spiesLost),
                    gameTime(actor));
        }

        if (!success) {
            newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.ACTOR_ONLY, actorId, targetId,
                    "Your intelligence operation against %s came back empty%s.".formatted(
                            target.getNation(), spiesLost > 0 ? " (spies compromised)" : ""),
                    gameTime(actor));
            return;
        }

        newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.ACTOR_ONLY, actorId, targetId,
                buildReport(target, targetId, clarity), gameTime(actor));
    }

    private String buildReport(PlayerEntity target, UUID targetId, double clarity) {
        double fraction = properties.getReconMaxUncertainty() * (1 - clarity);
        PlayerMilitaryEntity mil = military(targetId);
        PlayerEconomyEntity eco = economy(targetId);
        StringBuilder techs = new StringBuilder();
        for (PlayerResearchEntity r : researchRepository.findByPlayerId(targetId)) {
            if (random.nextDouble() < clarity) {
                techs.append(techs.isEmpty() ? "" : ", ").append(r.getResearchType());
            }
        }
        return ("INTEL on %s (%.0f%% confidence) — Army: infantry %s, tanks %s, stealth bombers %s, special ops %s, "
                + "air defense %s, missile batteries %s. Economy: farms %s, factories %s, robot factories %s, "
                + "banks %s. Tech detected: %s.").formatted(
                target.getNation(), clarity * 100,
                range(mil.getInfantry(), fraction), range(mil.getTanks(), fraction),
                range(mil.getStealthBombers(), fraction), range(mil.getSpecialOps(), fraction),
                range(mil.getAirDefenseBatteries(), fraction), range(mil.getMissileBatteries(), fraction),
                range(eco.getFarms(), fraction), range(eco.getFactories(), fraction),
                range(eco.getRobotFactories(), fraction), range(eco.getBanks(), fraction),
                techs.isEmpty() ? "none" : techs.toString());
    }

    private String range(int value, double fraction) {
        int halfWidth = (int) Math.round(value * fraction);
        return Math.max(0, value - halfWidth) + "-" + (value + halfWidth);
    }

    @Override
    @Transactional
    public void assassinateLeader(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        PlayerMilitaryEntity attackerMil = military(actorId);
        int specialOps = attackerMil.getSpecialOps();
        double opsBonus = specialOps * specialForcesProperties.getSpecialOpsAssassinationWeight();

        boolean vulnerable = target.getPopularity() < popularityProperties.getVulnerabilityThreshold();
        boolean success = vulnerable && attemptSucceeds(actorId, targetId, protectionService.defenseScore(targetId), opsBonus);

        int opsLost = Math.min(specialOps, (int) Math.round(
                protectionService.bodyguards(targetId) * specialForcesProperties.getSpecialOpsLossPerBodyguard()));
        if (opsLost > 0) {
            attackerMil.setSpecialOps(specialOps - opsLost);
            militaryRepository.save(attackerMil);
        }

        if (!success) {
            recordFailure(actor, target, "assassination");
            return;
        }
        leaderKillService.attemptKill(actor, target, NewsType.ESPIONAGE, "assassination");
    }

    private boolean attemptSucceeds(UUID actorId, UUID targetId) {
        return attemptSucceeds(actorId, targetId, 0.0, 0.0);
    }

    private boolean attemptSucceeds(UUID actorId, UUID targetId, double extraDefense) {
        return attemptSucceeds(actorId, targetId, extraDefense, 0.0);
    }

    private boolean attemptSucceeds(UUID actorId, UUID targetId, double extraDefense, double extraAttack) {
        double spies = espionage(actorId).getSpies() * researchModifierService.espionageMultiplier(actorId) + extraAttack;
        double counterIntel = espionage(targetId).getCounterIntel()
                * researchModifierService.counterIntelMultiplier(targetId);
        double probability = spies / (spies + counterIntel + extraDefense + properties.getBaseResistance());
        return random.nextDouble() < probability;
    }

    private void record(PlayerEntity actor, PlayerEntity target, String headline) {
        newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.INVOLVED_ONLY,
                actor.getId(), target.getId(), headline, gameTime(actor));
        log.info("Espionage by {} against {} succeeded", actor.getId(), target.getId());
    }

    private void recordFailure(PlayerEntity actor, PlayerEntity target, String operation) {
        newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.INVOLVED_ONLY,
                actor.getId(), target.getId(),
                "%s detected and repelled a %s from %s.".formatted(target.getNation(), operation, actor.getNation()),
                gameTime(actor));
    }

    private int scaled(int units, double fraction) {
        return Math.min(units, (int) Math.round(units * fraction));
    }

    private int victimPenalty(UUID targetId, int basePenalty) {
        return (int) Math.round(basePenalty * researchModifierService.popularityDamageMultiplier(targetId));
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

    private PlayerEspionageEntity espionage(UUID playerId) {
        return espionageRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player espionage not found: " + playerId));
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
