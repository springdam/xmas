package com.xmas.game.actions.handler.research;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;

/**
 * Base for research actions: one-time, charges on begin and records the
 * permanent modifier on completion.
 */
@RequiredArgsConstructor
public abstract class AbstractResearchHandler implements ActionHandler {

    protected final PlayerStateGateway state;
    protected final ResearchGateway research;

    @Override
    public void validate(ActionContext ctx) {
        if (research.hasResearch(ctx.getActorPlayerId(), type())) {
            throw new BadRequestException(type() + " has already been researched");
        }
        // One project at a time: the tech tree takes longer than a game, so picks matter.
        state.pendingResearch(ctx.getActorPlayerId()).ifPresent(busy -> {
            throw new BadRequestException("Your scientists are still working on " + busy);
        });
        if (state.getCash(ctx.getActorPlayerId()) < ctx.getDefinition().getCostCash()) {
            throw new BadRequestException("Insufficient funds for " + type());
        }
    }

    @Override
    public void begin(ActionContext ctx) {
        state.charge(ctx.getActorPlayerId(), ctx.getDefinition().getCostCash());
    }

    @Override
    public void onComplete(ActionContext ctx) {
        research.addResearch(ctx.getActorPlayerId(), type());
    }
}
