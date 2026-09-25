package com.xmas.game.actions.domain;

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
}
