package com.xmas.game.actions.handler.diplomacy;

import com.xmas.game.actions.diplomacy.DiplomacyResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractTargetedActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class PraiseLeaderHandler extends AbstractTargetedActionHandler {

    private final DiplomacyResolver diplomacyResolver;

    public PraiseLeaderHandler(PlayerStateGateway state, DiplomacyResolver diplomacyResolver) {
        super(state);
        this.diplomacyResolver = diplomacyResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.PRAISE_LEADER;
    }

    @Override
    public void onComplete(ActionContext ctx) {
        diplomacyResolver.praiseLeader(ctx.getActorPlayerId(), ctx.getTargetPlayerId());
    }
}
