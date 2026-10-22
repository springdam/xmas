package com.xmas.game.engine.combat;

import com.xmas.game.actions.combat.CombatResolver;
import com.xmas.game.combat.domain.CombatForce;
import com.xmas.game.combat.domain.CombatOutcome;
import com.xmas.game.combat.domain.UnitType;
import com.xmas.game.combat.service.CombatCalculator;
import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.config.SpecialForcesProperties;
import com.xmas.game.engine.domain.Popularity;
import com.xmas.game.engine.economy.BuildingDamageService;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.protection.LeaderKillService;
import com.xmas.game.engine.protection.ProtectionService;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerRepository;
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
public class JpaCombatResolver implements CombatResolver {

    private final PlayerRepository playerRepository;
    private final PlayerMilitaryRepository militaryRepository;
    private final GameRepository gameRepository;
    private final CombatCalculator combatCalculator;
    private final ResearchModifierService researchModifierService;
    private final ProtectionService protectionService;
    private final LeaderKillService leaderKillService;
    private final PopularityProperties popularityProperties;
    private final SpecialForcesProperties specialForcesProperties;
    private final NewsService newsService;
    private final PlayerEconomyRepository economyRepository;
    private final BuildingDamageService buildingDamageService;
    private final EconomyProperties economyProperties;

    @Override
    @Transactional
    public void announceAttack(UUID attackerPlayerId, UUID defenderPlayerId) {
        PlayerEntity attacker = player(attackerPlayerId);
        PlayerEntity defender = player(defenderPlayerId);
        newsService.record(attacker.getGameId(), NewsType.ATTACK, NewsVisibility.PUBLIC,
                attackerPlayerId, defenderPlayerId,
                "%s's forces are on the move against %s.".formatted(attacker.getNation(), defender.getNation()),
                gameTime(attacker));
    }

    @Override
    @Transactional
    public void resolveAttack(UUID attackerPlayerId, UUID defenderPlayerId, int quantity) {
        PlayerEntity attacker = player(attackerPlayerId);
        PlayerEntity defender = player(defenderPlayerId);
        PlayerMilitaryEntity attackerMil = military(attackerPlayerId);
        PlayerMilitaryEntity defenderMil = military(defenderPlayerId);

        double attackerMultiplier = researchModifierService.combatMultiplier(attackerPlayerId);
        double defenderMultiplier = researchModifierService.combatDefenseMultiplier(defenderPlayerId);
        CombatOutcome outcome = combatCalculator.resolve(
                force(attackerMil), force(defenderMil), attackerMultiplier, defenderMultiplier);

        applyLosses(attackerMil, outcome, true);
        applyLosses(defenderMil, outcome, false);
        militaryRepository.save(attackerMil);
        militaryRepository.save(defenderMil);
        BuildingDamageService.Losses buildingsLost = null;
        if (outcome.isAttackerVictory()) {
            // A beaten defender's economy takes damage in proportion to how badly they lost.
            PlayerEconomyEntity economy = economy(defenderPlayerId);
            buildingsLost = buildingDamageService.strike(economy,
                    outcome.getDefenderLossFraction() * economyProperties.getBattleDamageScale());
            economyRepository.save(economy);
            // Repeated defeats hurt less: the public is already braced for bad news.
            int penalty = (int) Math.round(popularityProperties.getAttackVictimPenalty()
                    * Math.pow(popularityProperties.getAttackPenaltyDecay(), defender.getWarStrain())
                    * researchModifierService.popularityDamageMultiplier(defenderPlayerId));
            defender.setPopularity(Popularity.clamp(defender.getPopularity() - penalty));
            defender.setWarStrain(defender.getWarStrain() + 1);
            playerRepository.save(defender);
        }

        newsService.record(attacker.getGameId(), NewsType.ATTACK, NewsVisibility.PUBLIC,
                attackerPlayerId, defenderPlayerId, headline(attacker, defender, outcome, buildingsLost),
                gameTime(attacker));
        log.info("Attack {} -> {} resolved (attackerVictory={})",
                attackerPlayerId, defenderPlayerId, outcome.isAttackerVictory());
    }

    @Override
    @Transactional
    public void resolveDecapitationStrike(UUID attackerPlayerId, UUID targetPlayerId) {
        PlayerEntity attacker = player(attackerPlayerId);
        PlayerEntity target = player(targetPlayerId);
        PlayerMilitaryEntity attackerMil = military(attackerPlayerId);
        PlayerMilitaryEntity targetMil = military(targetPlayerId);

        int negated = targetMil.getAirDefenseBatteries() * specialForcesProperties.getAirDefenseNegationPerBattery();
        int effectiveBombers = Math.max(0, attackerMil.getStealthBombers() - negated);
        // Ground forces help with diminishing returns: a huge army alone can't brute-force the strike.
        double strikePower = (Math.sqrt(attackerMil.getInfantry() + attackerMil.getTanks())
                * specialForcesProperties.getGroundDecapWeight()
                + effectiveBombers * specialForcesProperties.getBomberDecapWeight())
                * researchModifierService.airSuperiorityMultiplier(attackerPlayerId);

        boolean vulnerable = target.getPopularity() < popularityProperties.getVulnerabilityThreshold();
        boolean reaches = vulnerable
                && combatCalculator.resolveDecapitation(strikePower, protectionService.defenseScore(targetPlayerId));

        int bombersLost = Math.min(attackerMil.getStealthBombers(),
                (int) Math.round(protectionService.bodyguards(targetPlayerId)
                        * specialForcesProperties.getBomberLossPerBodyguard()));
        attackerMil.setStealthBombers(attackerMil.getStealthBombers() - bombersLost);
        militaryRepository.save(attackerMil);

        if (!reaches) {
            newsService.record(attacker.getGameId(), NewsType.ATTACK, NewsVisibility.PUBLIC,
                    attackerPlayerId, targetPlayerId,
                    "%s's decapitation strike on %s was thwarted (%d bombers lost).".formatted(
                            attacker.getNation(), target.getNation(), bombersLost),
                    gameTime(attacker));
            return;
        }
        leaderKillService.attemptKill(attacker, target, NewsType.ATTACK, "decapitation strike");
    }

    private void applyLosses(PlayerMilitaryEntity mil, CombatOutcome outcome, boolean attackerSide) {
        mil.setInfantry(mil.getInfantry() - lost(outcome, attackerSide, UnitType.INFANTRY));
        mil.setTanks(mil.getTanks() - lost(outcome, attackerSide, UnitType.TANK));
        mil.setStealthBombers(mil.getStealthBombers() - lost(outcome, attackerSide, UnitType.STEALTH_BOMBER));
        mil.setSpecialOps(mil.getSpecialOps() - lost(outcome, attackerSide, UnitType.SPECIAL_OPS));
    }

    private int lost(CombatOutcome outcome, boolean attackerSide, UnitType type) {
        return attackerSide ? outcome.attackerLost(type) : outcome.defenderLost(type);
    }

    private String headline(PlayerEntity attacker, PlayerEntity defender, CombatOutcome outcome,
                            BuildingDamageService.Losses buildingsLost) {
        String ruins = buildingsLost == null || !buildingsLost.any() ? ""
                : " Destroyed in %s: %s.".formatted(defender.getNation(), BuildingDamageService.describe(buildingsLost));
        return "%s attacked %s%s. Defender lost %d inf / %d tanks / %d bombers / %d ops; attacker lost %d inf / %d tanks / %d bombers / %d ops."
                .formatted(
                        attacker.getNation(), defender.getNation(),
                        outcome.isAttackerVictory() ? " and broke through" : " but was repelled",
                        outcome.defenderLost(UnitType.INFANTRY), outcome.defenderLost(UnitType.TANK),
                        outcome.defenderLost(UnitType.STEALTH_BOMBER), outcome.defenderLost(UnitType.SPECIAL_OPS),
                        outcome.attackerLost(UnitType.INFANTRY), outcome.attackerLost(UnitType.TANK),
                        outcome.attackerLost(UnitType.STEALTH_BOMBER), outcome.attackerLost(UnitType.SPECIAL_OPS))
                + ruins;
    }

    private CombatForce force(PlayerMilitaryEntity mil) {
        return CombatForce.builder()
                .infantry(mil.getInfantry())
                .tanks(mil.getTanks())
                .stealthBombers(mil.getStealthBombers())
                .specialOps(mil.getSpecialOps())
                .build();
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

    private PlayerEconomyEntity economy(UUID playerId) {
        return economyRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player economy not found: " + playerId));
    }

    private PlayerMilitaryEntity military(UUID playerId) {
        return militaryRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player military not found: " + playerId));
    }
}
