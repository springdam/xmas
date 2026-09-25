package com.xmas.game.combat.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CombatForce {

    private final int infantry;
    private final int tanks;
    private final int stealthBombers;
    private final int specialOps;

    public int count(UnitType type) {
        return switch (type) {
            case INFANTRY -> infantry;
            case TANK -> tanks;
            case STEALTH_BOMBER -> stealthBombers;
            case SPECIAL_OPS -> specialOps;
        };
    }

    public int total() {
        return infantry + tanks + stealthBombers + specialOps;
    }
}
