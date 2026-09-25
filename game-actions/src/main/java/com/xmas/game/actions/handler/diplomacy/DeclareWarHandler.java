package com.xmas.game.actions.handler.diplomacy;

import com.xmas.game.actions.diplomacy.DiplomacyResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractTargetedActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class DeclareWarHandler extends AbstractTargetedActionHandler {

    private final DiplomacyResolver diplomacyResolver;

    public DeclareWarHandler(PlayerStateGateway state, DiplomacyResolver diplomacyResolver) {
        super(state);
        this.diplomacyResolver = diplomacyResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.DECLARE_WAR;
    }

    @Override
    public void onComplete(ActionContext ctx) {
        diplomacyResolver.declareWar(ctx.getActorPlayerId(), ctx.getTargetPlayerId());
    }
}
