package com.xmas.game.engine.domain;

/** The six parts of a nation's throne room; each has a tier from 0 (bare) to 4 (gold). */
public enum PalacePart {
    WALLS(true),
    FLOOR(false),
    THRONE(false),
    PILLARS(true),
    WINDOWS(true),
    TREASURES(true);

    private final boolean plural;

    PalacePart(boolean plural) {
        this.plural = plural;
    }

    /** "a golden throne", "marble pillars": for news headlines. */
    public String described(String material) {
        String text = material + " " + name().toLowerCase();
        if (plural) {
            return text;
        }
        return (material.matches("^[aeiou].*") ? "an " : "a ") + text;
    }

    /** "the throne", "the pillars": for news headlines. */
    public String label() {
        return "the " + name().toLowerCase();
    }
}
