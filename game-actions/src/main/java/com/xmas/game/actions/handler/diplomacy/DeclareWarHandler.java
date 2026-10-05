package com.xmas.game.actions.handler.diplomacy;

import com.xmas.game.actions.diplomacy.DiplomacyResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractTargetedActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
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
    public void validate(ActionContext ctx) {
        super.validate(ctx);
        if (diplomacyResolver.atWar(ctx.getActorPlayerId(), ctx.getTargetPlayerId())) {
            throw new BadRequestException("You are already at war with that nation");
        }
    }

    @Override
    public void onComplete(ActionContext ctx) {
        diplomacyResolver.declareWar(ctx.getActorPlayerId(), ctx.getTargetPlayerId());
    }
}
