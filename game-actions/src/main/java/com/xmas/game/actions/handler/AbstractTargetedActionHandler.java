package com.xmas.game.actions.handler;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Base for targeted actions that charge the actor on begin and apply their
 * effect on completion. Subclasses implement type() and onComplete().
 */
@RequiredArgsConstructor
public abstract class AbstractTargetedActionHandler implements ActionHandler {

    protected final PlayerStateGateway state;

    /**
     * Actions that, while one is under way against the same target, block this one.
     * Empty means any number may run in parallel.
     */
    protected Set<ActionType> exclusiveWith() {
        return Set.of();
    }

    protected String alreadyUnderWayMessage() {
        return "Your last " + type() + " against that nation is still under way";
    }

    protected long totalCost(ActionContext ctx) {
        return ctx.getDefinition().getCostCash() * ctx.getQuantity();
    }

    @Override
    public void validate(ActionContext ctx) {
        if (ctx.getActorPlayerId().equals(ctx.getTargetPlayerId())) {
            throw new BadRequestException("A player cannot target themselves with " + type());
        }
        Set<ActionType> exclusive = exclusiveWith();
        if (!exclusive.isEmpty()
                && state.hasPendingAgainst(ctx.getActorPlayerId(), exclusive, ctx.getTargetPlayerId())) {
            throw new BadRequestException(alreadyUnderWayMessage());
        }
        if (state.getCash(ctx.getActorPlayerId()) < totalCost(ctx)) {
            throw new BadRequestException("Insufficient funds for " + type());
        }
    }

    @Override
    public void begin(ActionContext ctx) {
        state.charge(ctx.getActorPlayerId(), totalCost(ctx));
    }
}
