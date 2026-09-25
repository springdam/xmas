package com.xmas.game.actions.handler.diplomacy;

import com.xmas.game.actions.combat.CombatResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractTargetedActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class AttackHandler extends AbstractTargetedActionHandler {

    private final CombatResolver combatResolver;

    public AttackHandler(PlayerStateGateway state, CombatResolver combatResolver) {
        super(state);
        this.combatResolver = combatResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.ATTACK;
    }

    @Override
    public void onComplete(ActionContext ctx) {
        combatResolver.resolveAttack(ctx.getActorPlayerId(), ctx.getTargetPlayerId(), ctx.getQuantity());
    }
}
