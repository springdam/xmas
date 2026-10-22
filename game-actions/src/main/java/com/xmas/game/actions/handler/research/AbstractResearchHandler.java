package com.xmas.game.actions.handler.research;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;

/**
 * Base for research actions. Each research raises the tech one level (up to its max level);
 * higher levels cost more and take longer. Charges on begin, applies the level on completion.
 */
@RequiredArgsConstructor
public abstract class AbstractResearchHandler implements ActionHandler {

    protected final PlayerStateGateway state;
    protected final ResearchGateway research;

    private int nextLevel(ActionContext ctx) {
        return research.level(ctx.getActorPlayerId(), type()) + 1;
    }

    @Override
    public void validate(ActionContext ctx) {
        int next = nextLevel(ctx);
        int maxLevel = Math.max(1, ctx.getDefinition().getMaxLevel());
        if (next > maxLevel) {
            throw new BadRequestException(maxLevel == 1
                    ? type() + " has already been researched"
                    : type() + " is already at its highest level (" + maxLevel + ")");
        }
        // One project at a time: the tech tree takes longer than a game, so picks matter.
        state.pendingResearch(ctx.getActorPlayerId()).ifPresent(busy -> {
            throw new BadRequestException("Your scientists are still working on " + busy);
        });
        if (state.getCash(ctx.getActorPlayerId()) < ctx.getDefinition().costForLevel(next)) {
            throw new BadRequestException("Insufficient funds for " + type());
        }
    }

    @Override
    public void begin(ActionContext ctx) {
        state.charge(ctx.getActorPlayerId(), ctx.getDefinition().costForLevel(nextLevel(ctx)));
    }

    @Override
    public long durationMillis(ActionContext ctx) {
        return ctx.getDefinition().durationForLevel(nextLevel(ctx));
    }

    @Override
    public void onComplete(ActionContext ctx) {
        // Never beyond the top level (e.g. if the same level was stolen while this was under way).
        if (research.level(ctx.getActorPlayerId(), type()) < Math.max(1, ctx.getDefinition().getMaxLevel())) {
            research.advanceResearch(ctx.getActorPlayerId(), type());
        }
    }
}
