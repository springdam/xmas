package com.xmas.game.actions.handler.army;

import com.xmas.game.actions.combat.CombatResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractTargetedActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class DecapitationStrikeHandler extends AbstractTargetedActionHandler {

    private final CombatResolver combatResolver;

    public DecapitationStrikeHandler(PlayerStateGateway state, CombatResolver combatResolver) {
        super(state);
        this.combatResolver = combatResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.DECAPITATION_STRIKE;
    }

    @Override
    protected boolean oneAtATimePerTarget() {
        return true;
    }

    @Override
    public void onComplete(ActionContext ctx) {
        combatResolver.resolveDecapitationStrike(ctx.getActorPlayerId(), ctx.getTargetPlayerId());
    }
}
