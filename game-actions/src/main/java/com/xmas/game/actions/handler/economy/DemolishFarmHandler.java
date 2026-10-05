package com.xmas.game.actions.handler.economy;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractDemolishHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class DemolishFarmHandler extends AbstractDemolishHandler {

    public DemolishFarmHandler(PlayerStateGateway state) {
        super(state);
    }

    @Override
    public ActionType type() {
        return ActionType.DEMOLISH_FARM;
    }

    @Override
    protected ActionType building() {
        return ActionType.BUILD_FARM;
    }
}
