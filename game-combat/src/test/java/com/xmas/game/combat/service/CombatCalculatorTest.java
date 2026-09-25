package com.xmas.game.combat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.xmas.game.combat.config.CombatProperties;
import com.xmas.game.combat.domain.CombatForce;
import com.xmas.game.combat.domain.CombatOutcome;
import com.xmas.game.combat.domain.UnitType;
import com.xmas.game.combat.random.RandomSource;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;

class CombatCalculatorTest {

    private CombatCalculator calculator(double... rolls) {
        Deque<Double> queue = new ArrayDeque<>();
        for (double r : rolls) {
            queue.add(r);
        }
        RandomSource random = () -> queue.isEmpty() ? 0.5 : queue.poll();
        return new CombatCalculator(new CombatProperties(), random);
    }

    @Test
    void strongAttackerWinsAndInflictsProportionallyMoreLosses() {
        CombatForce attacker = CombatForce.builder().infantry(100).tanks(50).build();
        CombatForce defender = CombatForce.builder().infantry(20).tanks(5).build();

        CombatOutcome outcome = calculator(0.1, 0.5).resolve(attacker, defender);

        assertThat(outcome.isAttackerVictory()).isTrue();
        assertThat(outcome.defenderLost(UnitType.INFANTRY)).isPositive();
        double defenderFraction = (double) outcome.defenderLost(UnitType.INFANTRY) / defender.getInfantry();
        double attackerFraction = (double) outcome.attackerLost(UnitType.INFANTRY) / attacker.getInfantry();
        assertThat(defenderFraction).isGreaterThan(attackerFraction);
    }

    @Test
    void rockPaperScissors_tanksBeatInfantryButLoseToBombers() {
        CombatForce tanks = CombatForce.builder().tanks(50).build();
        CombatForce infantry = CombatForce.builder().infantry(50).build();

        // Tanks attacking infantry (tanks counter infantry) -> attacker wins on a neutral roll.
        assertThat(calculator(0.5, 0.5).resolve(tanks, infantry).isAttackerVictory()).isTrue();
        // Infantry attacking tanks (infantry is countered by tanks) -> attacker loses on the same roll.
        assertThat(calculator(0.5, 0.5).resolve(infantry, tanks).isAttackerVictory()).isFalse();
    }

    @Test
    void lossesNeverExceedAvailableUnits() {
        CombatForce attacker = CombatForce.builder().infantry(1).build();
        CombatForce defender = CombatForce.builder().infantry(1000).tanks(1000).build();

        CombatOutcome outcome = calculator(0.99, 0.99).resolve(attacker, defender);

        assertThat(outcome.attackerLost(UnitType.INFANTRY)).isLessThanOrEqualTo(1);
        assertThat(outcome.defenderLost(UnitType.INFANTRY)).isLessThanOrEqualTo(1000);
    }

    @Test
    void emptyForcesProduceNoLosses() {
        CombatOutcome outcome = calculator(0.5, 0.5)
                .resolve(CombatForce.builder().build(), CombatForce.builder().build());

        assertThat(outcome.isAttackerVictory()).isFalse();
        assertThat(outcome.defenderLost(UnitType.INFANTRY)).isZero();
    }

    @Test
    void decapitationProbabilityRisesWithStrikePower() {
        // With a roll of 0.5: strong strike (power 50) succeeds, weak strike (power 1) fails.
        assertThat(calculator(0.5).resolveDecapitation(50, 5)).isTrue();
        assertThat(calculator(0.5).resolveDecapitation(1, 5)).isFalse();
    }
}
