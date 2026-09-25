package com.xmas.game.actions.domain;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActionResult {

    private final ActionType actionType;
    private final boolean completedImmediately;
    private final Instant completesAt;
    private final String message;
}
