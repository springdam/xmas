package com.xmas.game.actions.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActionContext {

    private final UUID actorPlayerId;
    private final UUID targetPlayerId;
    private final int quantity;
    private final ActionDefinition definition;
}
