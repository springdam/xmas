package com.xmas.game.actions.handler;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;

/**
 * Base for demolishing economy buildings: free, no refund, and the plot of land is freed
 * so the nation can build something else. Only standing buildings can be demolished.
 */
@RequiredArgsConstructor
public abstract class AbstractDemolishHandler implements ActionHandler {

    protected final PlayerStateGateway state;

    /** The building this action tears down (its BUILD_ action type). */
    protected abstract ActionType building();

    @Override
    public void validate(ActionContext ctx) {
        if (ctx.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        int standing = state.getAssetCount(ctx.getActorPlayerId(), building());
        if (ctx.getQuantity() > standing) {
            throw new BadRequestException(standing == 0
                    ? "Nothing to demolish"
                    : "Only " + standing + " standing to demolish");
        }
    }

    @Override
    public void begin(ActionContext ctx) {
        // Demolition is free.
    }

    @Override
    public void onComplete(ActionContext ctx) {
        state.addAsset(ctx.getActorPlayerId(), building(), -ctx.getQuantity());
    }
}
