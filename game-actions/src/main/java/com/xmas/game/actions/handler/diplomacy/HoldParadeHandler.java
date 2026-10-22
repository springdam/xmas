package com.xmas.game.actions.handler.diplomacy;

import com.xmas.game.actions.diplomacy.DiplomacyResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HoldParadeHandler implements ActionHandler {

    private final PlayerStateGateway state;
    private final DiplomacyResolver diplomacyResolver;

    @Override
    public ActionType type() {
        return ActionType.HOLD_PARADE;
    }

    @Override
    public void validate(ActionContext ctx) {
        if (state.getCash(ctx.getActorPlayerId()) < ctx.getDefinition().getCostCash()) {
            throw new BadRequestException("Insufficient funds to hold a parade");
        }
        diplomacyResolver.assertCanHoldParade(ctx.getActorPlayerId());
    }

    @Override
    public void begin(ActionContext ctx) {
        state.charge(ctx.getActorPlayerId(), ctx.getDefinition().getCostCash());
    }

    @Override
    public void onComplete(ActionContext ctx) {
        diplomacyResolver.holdParade(ctx.getActorPlayerId());
    }
}
