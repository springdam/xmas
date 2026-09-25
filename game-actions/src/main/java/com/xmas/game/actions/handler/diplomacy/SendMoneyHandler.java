package com.xmas.game.actions.handler.diplomacy;

import com.xmas.game.actions.diplomacy.DiplomacyResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractTargetedActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

/**
 * Transfers cash between players. The action's {@code quantity} is the dollar
 * amount; with cost-cash configured to 1 the base handler charges exactly that
 * amount on begin, and the resolver credits the target on completion.
 */
@Component
public class SendMoneyHandler extends AbstractTargetedActionHandler {

    private final DiplomacyResolver diplomacyResolver;

    public SendMoneyHandler(PlayerStateGateway state, DiplomacyResolver diplomacyResolver) {
        super(state);
        this.diplomacyResolver = diplomacyResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.SEND_MONEY;
    }

    @Override
    public void onComplete(ActionContext ctx) {
        diplomacyResolver.sendMoney(ctx.getActorPlayerId(), ctx.getTargetPlayerId(), ctx.getQuantity());
    }
}
