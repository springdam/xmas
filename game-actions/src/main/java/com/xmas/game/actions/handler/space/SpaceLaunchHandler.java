package com.xmas.game.actions.handler.space;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.space.SpaceGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Launches the finished spaceship to Mars: the launcher wins and the game is over. */
@Component
@RequiredArgsConstructor
public class SpaceLaunchHandler implements ActionHandler {

    private final PlayerStateGateway state;
    private final SpaceGateway space;

    @Override
    public ActionType type() {
        return ActionType.SPACE_LAUNCH;
    }

    @Override
    public void validate(ActionContext ctx) {
        if (!space.shipComplete(ctx.getActorPlayerId())) {
            throw new BadRequestException("Your spaceship isn't finished: build every part first");
        }
        if (state.getCash(ctx.getActorPlayerId()) < ctx.getDefinition().getCostCash()) {
            throw new BadRequestException("Insufficient funds to fuel the launch");
        }
    }

    @Override
    public void begin(ActionContext ctx) {
        state.charge(ctx.getActorPlayerId(), ctx.getDefinition().getCostCash());
    }

    @Override
    public void onComplete(ActionContext ctx) {
        space.launch(ctx.getActorPlayerId());
    }
}
