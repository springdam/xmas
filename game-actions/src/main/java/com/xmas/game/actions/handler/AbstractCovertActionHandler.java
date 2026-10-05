package com.xmas.game.actions.handler;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.UUID;

/**
 * Base for spy operations. With no agents the odds are zero, so they are refused up front
 * rather than charged for a certain failure.
 */
public abstract class AbstractCovertActionHandler extends AbstractTargetedActionHandler {

    protected AbstractCovertActionHandler(PlayerStateGateway state) {
        super(state);
    }

    @Override
    public void validate(ActionContext ctx) {
        super.validate(ctx);
        if (!hasAgents(ctx.getActorPlayerId())) {
            throw new BadRequestException(type() + " needs " + agentsRequired());
        }
    }

    /** Whether the actor has anyone to send. Spies, unless a subclass accepts other agents. */
    protected boolean hasAgents(UUID actorId) {
        return state.getAssetCount(actorId, ActionType.HIRE_SPIES) > 0;
    }

    protected String agentsRequired() {
        return "at least one spy";
    }
}
