package com.xmas.game.actions.handler.specialweapon;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractTargetedActionHandler;
import com.xmas.game.actions.specialweapon.SpecialWeaponResolver;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class SpecialWeaponAttackHandler extends AbstractTargetedActionHandler {

    private final SpecialWeaponResolver specialWeaponResolver;

    public SpecialWeaponAttackHandler(PlayerStateGateway state, SpecialWeaponResolver specialWeaponResolver) {
        super(state);
        this.specialWeaponResolver = specialWeaponResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.SPECIAL_WEAPON_ATTACK;
    }

    @Override
    public void validate(ActionContext ctx) {
        super.validate(ctx);
        int nukes = state.getAssetCount(ctx.getActorPlayerId(), ActionType.BUILD_NUKE);
        if (nukes < ctx.getQuantity()) {
            throw new BadRequestException("Not enough warheads to launch");
        }
    }

    @Override
    public void onComplete(ActionContext ctx) {
        specialWeaponResolver.launch(ctx.getActorPlayerId(), ctx.getTargetPlayerId(), ctx.getQuantity());
    }
}
