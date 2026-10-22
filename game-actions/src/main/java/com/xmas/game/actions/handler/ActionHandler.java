package com.xmas.game.actions.handler;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;

public interface ActionHandler {

    ActionType type();

    void validate(ActionContext ctx);

    void begin(ActionContext ctx);

    void onComplete(ActionContext ctx);

    /** How long this particular action takes; research overrides it with the next level's duration. */
    default long durationMillis(ActionContext ctx) {
        return ctx.getDefinition().getDurationMillis();
    }
}
