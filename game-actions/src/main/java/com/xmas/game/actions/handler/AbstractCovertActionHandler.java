package com.xmas.game.actions.handler;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Base for spy operations. With no agents the odds are zero, so they are refused up front
 * rather than charged for a certain failure.
 */
public abstract class AbstractCovertActionHandler extends AbstractTargetedActionHandler {

    protected AbstractCovertActionHandler(PlayerStateGateway state) {
        super(state);
    }

    /** Spy operations: the whole spy force goes on each, so only one runs against a nation at a time. */
    public static final Set<ActionType> SPY_OPERATIONS = EnumSet.of(
            ActionType.PERFORM_CYBER_ATTACK, ActionType.PERFORM_SABOTAGE, ActionType.PERFORM_HEIST,
            ActionType.STEAL_TECHNOLOGY, ActionType.GATHER_INTELLIGENCE, ActionType.ASSASSINATE_LEADER);

    @Override
    protected Set<ActionType> exclusiveWith() {
        return SPY_OPERATIONS;
    }

    @Override
    protected String alreadyUnderWayMessage() {
        return "Your spies are still on an operation against that nation";
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
