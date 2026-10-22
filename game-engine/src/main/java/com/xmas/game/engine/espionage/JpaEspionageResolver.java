package com.xmas.game.engine.espionage;

import com.xmas.game.actions.espionage.EspionageResolver;
import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.config.EspionageProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.config.SpecialForcesProperties;
import com.xmas.game.engine.domain.Popularity;
import com.xmas.game.engine.economy.BuildingDamageService;
import com.xmas.game.engine.economy.IncomeService;
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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.xmas.game.engine.palace.PalaceService;

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
    private final BuildingDamageService buildingDamageService;
    private final IncomeService incomeService;
    private final PalaceService palaceService;

    @Override
    @Transactional
    public void performHeist(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        Attempt attempt = attempt(actorId, targetId, 0.0, 0.0);
        if (!attempt.isSuccess()) {
            recordFailure(actor, target, "a heist", attempt.getSpiesCaught());
            return;
        }
        long incomeCap = Math.round(incomeService.perTick(targetId, economy(targetId), target.getPopularity())
                * properties.getHeistMaxIncomeTicks());
        long stolen = Math.min(Math.round(target.getCash() * properties.getHeistCashFraction()), incomeCap);
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
        Attempt attempt = attempt(actorId, targetId, 0.0, 0.0);
        if (!attempt.isSuccess()) {
            recordFailure(actor, target, "sabotage", attempt.getSpiesCaught());
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
        palaceService.onSabotaged(targetId);
        record(actor, target, "%s sabotaged %s's forces, destroying %d infantry and %s.".formatted(
                actor.getNation(), target.getNation(), infantryLost, count(tanksLost, "tank", "tanks")));
    }

    @Override
    @Transactional
    public void vandalizePalace(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        Attempt attempt = attempt(actorId, targetId, 0.0, 0.0);
        if (!attempt.isSuccess()) {
            recordFailure(actor, target, "palace vandalism", attempt.getSpiesCaught());
            return;
        }
        palaceService.onVandalized(targetId);
        record(actor, target, "%s's agents vandalized %s's palace.".formatted(actor.getNation(), target.getNation()));
    }

    @Override
    @Transactional
    public void performCyberAttack(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        Attempt attempt = attempt(actorId, targetId, 0.0, 0.0);
        if (!attempt.isSuccess()) {
            recordFailure(actor, target, "a cyber attack", attempt.getSpiesCaught());
            return;
        }
        PlayerEconomyEntity economy = economy(targetId);
        BuildingDamageService.Losses losses = buildingDamageService.strike(economy, properties.getCyberFraction());
        economyRepository.save(economy);
        target.setPopularity(Popularity.clamp(target.getPopularity() - victimPenalty(targetId, popularityProperties.getCyberVictimPenalty())));
        playerRepository.save(target);
        record(actor, target, "%s's cyber attack crippled %s's economy, destroying %s."
                .formatted(actor.getNation(), target.getNation(), BuildingDamageService.describe(losses)));
    }

    @Override
    @Transactional
    public void stealTechnology(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        Attempt attempt = attempt(actorId, targetId, 0.0, 0.0);
        if (!attempt.isSuccess()) {
            recordFailure(actor, target, "technology theft", attempt.getSpiesCaught());
            return;
        }
        // One level of one tech the target is ahead in, picked at random.
        List<ActionType> ahead = researchRepository.findByPlayerId(targetId).stream()
                .filter(r -> r.getLevel() > researchGateway.level(actorId, r.getResearchType()))
                .map(PlayerResearchEntity::getResearchType)
                .sorted()
                .toList();
        if (ahead.isEmpty()) {
            record(actor, target, "%s infiltrated %s but found no new technology to steal.".formatted(
                    actor.getNation(), target.getNation()));
            return;
        }
        ActionType stolen = ahead.get(Math.min(ahead.size() - 1, (int) (random.nextDouble() * ahead.size())));
        researchGateway.advanceResearch(actorId, stolen);
        record(actor, target, "%s stole %s (level %d) from %s.".formatted(
                actor.getNation(), stolen, researchGateway.level(actorId, stolen), target.getNation()));
    }

    @Override
    @Transactional
    public void gatherIntelligence(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        Attempt attempt = attempt(actorId, targetId, 0.0, 0.0);
        if (!attempt.isSuccess()) {
            recordFailure(actor, target, "an intelligence operation", attempt.getSpiesCaught());
            return;
        }
        // A clean success: the target never learns. Clarity sets how tight the ranges are.
        newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.ACTOR_ONLY, actorId, targetId,
                buildReport(target, targetId, attempt.getSuccessChance()), gameTime(actor));
    }

    private String buildReport(PlayerEntity target, UUID targetId, double clarity) {
        double fraction = properties.getReconMaxUncertainty() * (1 - clarity);
        PlayerMilitaryEntity mil = military(targetId);
        PlayerEconomyEntity eco = economy(targetId);
        StringBuilder techs = new StringBuilder();
        for (PlayerResearchEntity r : researchRepository.findByPlayerId(targetId)) {
            if (random.nextDouble() < clarity) {
                techs.append(techs.isEmpty() ? "" : ", ").append(r.getResearchType()).append(" L").append(r.getLevel());
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
        Attempt attempt = attempt(actorId, targetId, protectionService.defenseScore(targetId), opsBonus);
        boolean success = vulnerable && attempt.isSuccess();

        int opsLost = Math.min(specialOps, (int) Math.round(
                protectionService.bodyguards(targetId) * specialForcesProperties.getSpecialOpsLossPerBodyguard()));
        if (opsLost > 0) {
            attackerMil.setSpecialOps(specialOps - opsLost);
            militaryRepository.save(attackerMil);
        }

        if (!success) {
            recordFailure(actor, target, "an assassination", attempt.getSpiesCaught());
            return;
        }
        leaderKillService.attemptKill(actor, target, NewsType.ESPIONAGE, "assassination");
    }

    /** The outcome of one spy operation. */
    @Value
    private static class Attempt {
        boolean success;
        int spiesCaught;
        double successChance;
    }

    /**
     * Rolls a spy operation: attack (spies, Cryptography, extra agents) against defense
     * (counter-intel, Counter-Intelligence Doctrine, extra protection). On failure each of
     * the actor's spies may be caught; they are removed here.
     */
    private Attempt attempt(UUID actorId, UUID targetId, double extraDefense, double extraAttack) {
        PlayerEspionageEntity actorEsp = espionage(actorId);
        double attack = actorEsp.getSpies() * researchModifierService.espionageMultiplier(actorId) + extraAttack;
        double defense = espionage(targetId).getCounterIntel()
                * researchModifierService.counterIntelMultiplier(targetId) + extraDefense;
        double chance = CovertOdds.successChance(attack, defense, properties.getBaseResistance());
        if (random.nextDouble() < chance) {
            return new Attempt(true, 0, chance);
        }
        int caught = CovertOdds.spiesCaught(actorEsp.getSpies(),
                CovertOdds.captureChance(attack, defense, properties.getSpyCaptureRate()), random);
        if (caught > 0) {
            actorEsp.setSpies(actorEsp.getSpies() - caught);
            espionageRepository.save(actorEsp);
        }
        return new Attempt(false, caught, chance);
    }

    private void record(PlayerEntity actor, PlayerEntity target, String headline) {
        newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.INVOLVED_ONLY,
                actor.getId(), target.getId(), headline, gameTime(actor));
        log.info("Espionage by {} against {} succeeded", actor.getId(), target.getId());
    }

    /**
     * News for a failed operation. Caught spies give the attacker away; if nobody was caught
     * the target only learns that someone tried, and the attacker hears it went unnoticed.
     */
    private void recordFailure(PlayerEntity actor, PlayerEntity target, String operation, int spiesCaught) {
        Instant when = gameTime(actor);
        if (spiesCaught > 0) {
            newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.INVOLVED_ONLY,
                    actor.getId(), target.getId(),
                    "%s caught %s from %s attempting %s.".formatted(target.getNation(),
                            count(spiesCaught, "spy", "spies"), actor.getNation(), operation),
                    when);
            return;
        }
        newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.INVOLVED_ONLY,
                null, target.getId(),
                "Foreign agents attempted %s against %s and failed.".formatted(operation, target.getNation()),
                when);
        newsService.record(actor.getGameId(), NewsType.ESPIONAGE, NewsVisibility.ACTOR_ONLY,
                actor.getId(), target.getId(),
                "%s against %s failed. None of your spies were caught.".formatted(
                        capitalize(operation), target.getNation()),
                when);
    }

    /** "1 tank", "3 tanks". */
    private static String count(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
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
