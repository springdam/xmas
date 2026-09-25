package com.xmas.game.actions.handler.espionage;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.espionage.EspionageResolver;
import com.xmas.game.actions.handler.AbstractTargetedActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class AssassinateLeaderHandler extends AbstractTargetedActionHandler {

    private final EspionageResolver espionageResolver;

    public AssassinateLeaderHandler(PlayerStateGateway state, EspionageResolver espionageResolver) {
        super(state);
        this.espionageResolver = espionageResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.ASSASSINATE_LEADER;
    }

    @Override
    public void onComplete(ActionContext ctx) {
        espionageResolver.assassinateLeader(ctx.getActorPlayerId(), ctx.getTargetPlayerId());
    }
}
