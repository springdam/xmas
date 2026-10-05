package com.xmas.game.engine.espionage;

import com.xmas.game.combat.random.RandomSource;

/**
 * The odds behind every spy operation. Attack is the actor's effective spies (plus any
 * extra agents), defense the target's effective counter-intelligence (plus any extra
 * protection); both already include research multipliers.
 */
public final class CovertOdds {

    private CovertOdds() {
    }

    /** Chance the operation succeeds: attack / (attack + defense + baseResistance). */
    public static double successChance(double attack, double defense, double baseResistance) {
        double total = attack + defense + baseResistance;
        return total <= 0 ? 0.0 : attack / total;
    }

    /**
     * Chance each spy is caught when the operation fails: captureRate * defense / (attack + defense).
     * The more outmatched the spies, the likelier capture; no defense means nobody is caught.
     */
    public static double captureChance(double attack, double defense, double captureRate) {
        double total = attack + defense;
        return total <= 0 ? 0.0 : captureRate * defense / total;
    }

    /** Rolls each spy independently against the capture chance. */
    public static int spiesCaught(int spies, double captureChance, RandomSource random) {
        int caught = 0;
        for (int i = 0; i < spies; i++) {
            if (random.nextDouble() < captureChance) {
                caught++;
            }
        }
        return caught;
    }
}
