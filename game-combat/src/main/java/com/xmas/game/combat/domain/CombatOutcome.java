package com.xmas.game.combat.domain;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CombatOutcome {

    private final boolean attackerVictory;
    private final Map<UnitType, Integer> attackerLosses;
    private final Map<UnitType, Integer> defenderLosses;
    /** Share of the defender's forces destroyed (0..1); also how hard a beaten defender's economy is hit. */
    private final double defenderLossFraction;

    public int attackerLost(UnitType type) {
        return attackerLosses.getOrDefault(type, 0);
    }

    public int defenderLost(UnitType type) {
        return defenderLosses.getOrDefault(type, 0);
    }
}
