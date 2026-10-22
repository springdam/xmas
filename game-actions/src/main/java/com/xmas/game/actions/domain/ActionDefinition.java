package com.xmas.game.actions.domain;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActionDefinition {

    private final ActionType actionType;
    private final ActionCategory category;
    private final long costCash;
    private final long durationMillis;
    private final boolean targeted;
    private final boolean publicVisibility;
    private final String description;
    /** Optional cap on how many of this asset a player may hold at once; null means no cap. */
    private final Integer maxTotal;
    /** Optional research the actor must have completed before this action is allowed; null means none. */
    private final ActionType requiredResearch;
    /** Space program only: the part that must be built first; null means none. */
    private final ActionType requiresBuilt;
    /** Research only: number of levels (1 = a one-off unlock). */
    private final int maxLevel;
    /** Research only: cost and duration of each level (index 0 = level 1). */
    private final List<Long> levelCosts;
    private final List<Long> levelDurationsMillis;

    /** Cost of reaching {@code level}; the base cost for non-research actions. */
    public long costForLevel(int level) {
        return levelCosts == null || levelCosts.isEmpty() ? costCash : levelCosts.get(clampLevel(level) - 1);
    }

    /** Duration of reaching {@code level}; the base duration for non-research actions. */
    public long durationForLevel(int level) {
        return levelDurationsMillis == null || levelDurationsMillis.isEmpty()
                ? durationMillis : levelDurationsMillis.get(clampLevel(level) - 1);
    }

    private int clampLevel(int level) {
        return Math.max(1, Math.min(level, levelCosts.size()));
    }
}
