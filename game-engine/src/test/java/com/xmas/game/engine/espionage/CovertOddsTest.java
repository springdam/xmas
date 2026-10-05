package com.xmas.game.engine.espionage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.xmas.game.combat.random.RandomSource;
import org.junit.jupiter.api.Test;

class CovertOddsTest {

    @Test
    void successChanceWeighsSpiesAgainstCounterIntelAndBaseResistance() {
        assertThat(CovertOdds.successChance(20, 5, 2)).isCloseTo(20.0 / 27, within(1e-9));
        assertThat(CovertOdds.successChance(20, 0, 2)).isCloseTo(20.0 / 22, within(1e-9));
        assertThat(CovertOdds.successChance(0, 5, 2)).isZero();
        assertThat(CovertOdds.successChance(0, 0, 0)).isZero();
    }

    @Test
    void captureChanceGrowsTheMoreOutmatchedTheSpiesAre() {
        assertThat(CovertOdds.captureChance(20, 5, 0.5)).isCloseTo(0.1, within(1e-9));
        assertThat(CovertOdds.captureChance(5, 20, 0.5)).isCloseTo(0.4, within(1e-9));
        assertThat(CovertOdds.captureChance(40, 40, 0.5)).isCloseTo(0.25, within(1e-9));
    }

    @Test
    void noDefenseCatchesNobody() {
        assertThat(CovertOdds.captureChance(20, 0, 0.5)).isZero();
        assertThat(CovertOdds.captureChance(0, 0, 0.5)).isZero();
    }

    @Test
    void eachSpyIsRolledIndependently() {
        double[] rolls = {0.1, 0.9, 0.2, 0.3, 0.99};
        int[] i = {0};
        RandomSource random = () -> rolls[i[0]++];

        // capture chance 0.25: rolls 0.1 and 0.2 are below it
        assertThat(CovertOdds.spiesCaught(5, 0.25, random)).isEqualTo(2);
        assertThat(i[0]).isEqualTo(5);
    }
}
