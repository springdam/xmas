package com.xmas.game.actions.handler.army;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractBuildHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class BuildMissileBatteryHandler extends AbstractBuildHandler {

    public BuildMissileBatteryHandler(PlayerStateGateway state) {
        super(state);
    }

    @Override
    public ActionType type() {
        return ActionType.BUILD_MISSILE_BATTERY;
    }
}
