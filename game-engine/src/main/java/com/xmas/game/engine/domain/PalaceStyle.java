package com.xmas.game.engine.domain;

import java.util.List;

/** The look of a nation's palace, chosen once at the start; each style names its five tiers its own way. */
public enum PalaceStyle {
    PRESIDENTIAL(List.of("bare", "clapboard", "plaster", "marble", "gilded")),
    CASTLE(List.of("bare", "wooden", "stone", "marble", "golden")),
    MIDDLE_EASTERN(List.of("bare", "adobe", "sandstone", "tiled", "golden")),
    ASIAN(List.of("bare", "bamboo", "lacquered", "jade", "golden"));

    private final List<String> tierNames;

    PalaceStyle(List<String> tierNames) {
        this.tierNames = tierNames;
    }

    /** "marble", "lacquered", ...: the material a tier is made of in this style. */
    public String tierName(int tier) {
        return tierNames.get(Math.max(0, Math.min(tier, tierNames.size() - 1)));
    }
}
