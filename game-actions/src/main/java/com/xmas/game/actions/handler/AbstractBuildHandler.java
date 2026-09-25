package com.xmas.game.actions.handler;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;

/**
 * Base for asset-producing actions: charge cash on begin, grant the produced
 * asset on completion. Immediate actions (duration 0) run both in one step;
 * timed actions have their onComplete driven by the game tick.
 */
@RequiredArgsConstructor
public abstract class AbstractBuildHandler implements ActionHandler {

    protected final PlayerStateGateway state;

    protected long totalCost(ActionContext ctx) {
        return ctx.getDefinition().getCostCash() * ctx.getQuantity();
    }

    @Override
    public void validate(ActionContext ctx) {
        if (ctx.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        Integer maxTotal = ctx.getDefinition().getMaxTotal();
        if (maxTotal != null
                && state.getAssetCount(ctx.getActorPlayerId(), type()) + ctx.getQuantity() > maxTotal) {
            throw new BadRequestException("Cannot hold more than " + maxTotal + " of " + type());
        }
        if (state.getCash(ctx.getActorPlayerId()) < totalCost(ctx)) {
            throw new BadRequestException("Insufficient funds for " + type());
        }
    }

    @Override
    public void begin(ActionContext ctx) {
        state.charge(ctx.getActorPlayerId(), totalCost(ctx));
    }

    @Override
    public void onComplete(ActionContext ctx) {
        state.addAsset(ctx.getActorPlayerId(), type(), ctx.getQuantity());
    }
}
