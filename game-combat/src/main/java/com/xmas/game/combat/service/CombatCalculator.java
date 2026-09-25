package com.xmas.game.combat.service;

import com.xmas.game.combat.config.CombatProperties;
import com.xmas.game.combat.domain.CombatForce;
import com.xmas.game.combat.domain.CombatOutcome;
import com.xmas.game.combat.domain.UnitType;
import com.xmas.game.combat.random.RandomSource;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CombatCalculator {

    private final CombatProperties properties;
    private final RandomSource random;

    public CombatOutcome resolve(CombatForce attacker, CombatForce defender) {
        return resolve(attacker, defender, 1.0);
    }

    public CombatOutcome resolve(CombatForce attacker, CombatForce defender, double attackerPowerMultiplier) {
        return resolve(attacker, defender, attackerPowerMultiplier, 1.0);
    }

    public CombatOutcome resolve(CombatForce attacker, CombatForce defender,
                                 double attackerPowerMultiplier, double defenderPowerMultiplier) {
        double attackPower = sidePower(attacker, defender, true) * attackerPowerMultiplier;
        double defensePower = sidePower(defender, attacker, false) * defenderPowerMultiplier;
        double total = attackPower + defensePower;
        if (total <= 0) {
            return CombatOutcome.builder()
                    .attackerVictory(false)
                    .attackerLosses(new EnumMap<>(UnitType.class))
                    .defenderLosses(new EnumMap<>(UnitType.class))
                    .build();
        }

        double attackerShare = attackPower / total;
        double victoryRoll = random.nextDouble();
        double casualtyRoll = random.nextDouble();

        double defenderLossFraction = clamp01(attackerShare * properties.getCasualtyScale() * (0.5 + victoryRoll));
        double attackerLossFraction = clamp01((1 - attackerShare) * properties.getCasualtyScale() * (0.5 + casualtyRoll));

        return CombatOutcome.builder()
                .attackerVictory(victoryRoll < attackerShare)
                .attackerLosses(losses(attacker, attackerLossFraction))
                .defenderLosses(losses(defender, defenderLossFraction))
                .build();
    }

    /**
     * A decapitation strike reaches the leader with probability driven by the strike power
     * (mainly stealth bombers, net of air defense) against the leadership's protection.
     */
    public boolean resolveDecapitation(double strikePower, double protectionDefense) {
        if (strikePower <= 0) {
            return false;
        }
        double probability = strikePower / (strikePower + protectionDefense + properties.getDecapitationResistance());
        return random.nextDouble() < probability;
    }

    private double sidePower(CombatForce force, CombatForce opposing, boolean attacking) {
        double power = 0;
        for (UnitType type : UnitType.values()) {
            double weight = attacking ? properties.attack(type) : properties.defense(type);
            power += force.count(type) * weight * averageCounter(type, opposing);
        }
        return power;
    }

    private double averageCounter(UnitType type, CombatForce opposing) {
        int opposingTotal = opposing.total();
        if (opposingTotal == 0) {
            return 1.0;
        }
        double weighted = 0;
        for (UnitType other : UnitType.values()) {
            weighted += (opposing.count(other) / (double) opposingTotal) * counterMultiplier(type, other);
        }
        return weighted;
    }

    private double counterMultiplier(UnitType unit, UnitType opponent) {
        if (unit.beats() == opponent) {
            return properties.getCounterBonus();
        }
        if (opponent.beats() == unit) {
            return properties.getCounterPenalty();
        }
        return 1.0;
    }

    private Map<UnitType, Integer> losses(CombatForce force, double fraction) {
        Map<UnitType, Integer> result = new EnumMap<>(UnitType.class);
        for (UnitType type : UnitType.values()) {
            int units = force.count(type);
            result.put(type, Math.min(units, (int) Math.round(units * fraction)));
        }
        return result;
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
