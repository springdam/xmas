package com.xmas.game.actions.handler.space;

import com.xmas.game.actions.domain.ActionCategory;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.space.SpaceGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;

/**
 * Builds one part of the Mars spaceship. Each part is built once, may need another part first
 * (requires-built), and only one part is under construction at a time (in parallel with research).
 */
@RequiredArgsConstructor
public class SpaceProjectHandler implements ActionHandler {

    private final ActionType part;
    private final PlayerStateGateway state;
    private final SpaceGateway space;

    @Override
    public ActionType type() {
        return part;
    }

    @Override
    public void validate(ActionContext ctx) {
        if (space.hasPart(ctx.getActorPlayerId(), part)) {
            throw new BadRequestException(part + " is already built");
        }
        ActionType before = ctx.getDefinition().getRequiresBuilt();
        if (before != null && !space.hasPart(ctx.getActorPlayerId(), before)) {
            throw new BadRequestException(part + " needs " + before + " first");
        }
        state.pendingIn(ctx.getActorPlayerId(), ActionCategory.SPACE).ifPresent(busy -> {
            throw new BadRequestException("Your engineers are still building " + busy);
        });
        if (state.getCash(ctx.getActorPlayerId()) < ctx.getDefinition().getCostCash()) {
            throw new BadRequestException("Insufficient funds for " + part);
        }
    }

    @Override
    public void begin(ActionContext ctx) {
        state.charge(ctx.getActorPlayerId(), ctx.getDefinition().getCostCash());
    }

    @Override
    public void onComplete(ActionContext ctx) {
        if (!space.hasPart(ctx.getActorPlayerId(), part)) {
            space.addPart(ctx.getActorPlayerId(), part);
        }
    }
}
