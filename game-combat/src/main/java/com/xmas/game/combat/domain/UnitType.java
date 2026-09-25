package com.xmas.game.combat.domain;

/**
 * The four combat unit types and their rock-paper-scissors cycle:
 * tanks beat infantry, stealth bombers beat tanks, special ops beat stealth bombers,
 * infantry beat special ops. Each unit is neutral against the remaining type.
 */
public enum UnitType {
    INFANTRY,
    TANK,
    STEALTH_BOMBER,
    SPECIAL_OPS;

    /** The unit type this one counters (does bonus damage against). */
    public UnitType beats() {
        return switch (this) {
            case TANK -> INFANTRY;
            case STEALTH_BOMBER -> TANK;
            case SPECIAL_OPS -> STEALTH_BOMBER;
            case INFANTRY -> SPECIAL_OPS;
        };
    }
}
