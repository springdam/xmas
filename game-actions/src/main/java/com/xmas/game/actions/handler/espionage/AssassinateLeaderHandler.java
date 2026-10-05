package com.xmas.game.actions.handler.espionage;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.espionage.EspionageResolver;
import com.xmas.game.actions.handler.AbstractCovertActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AssassinateLeaderHandler extends AbstractCovertActionHandler {

    private final EspionageResolver espionageResolver;

    public AssassinateLeaderHandler(PlayerStateGateway state, EspionageResolver espionageResolver) {
        super(state);
        this.espionageResolver = espionageResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.ASSASSINATE_LEADER;
    }

    /** Special ops can carry out an assassination without spies. */
    @Override
    protected boolean hasAgents(UUID actorId) {
        return super.hasAgents(actorId) || state.getAssetCount(actorId, ActionType.RECRUIT_SPECIAL_OPS) > 0;
    }

    @Override
    protected String agentsRequired() {
        return "at least one spy or special ops unit";
    }

    @Override
    public void onComplete(ActionContext ctx) {
        espionageResolver.assassinateLeader(ctx.getActorPlayerId(), ctx.getTargetPlayerId());
    }
}
