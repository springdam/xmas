package com.xmas.game.actions.handler.economy;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractBuildHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class BuildRobotFactoryHandler extends AbstractBuildHandler {

    public BuildRobotFactoryHandler(PlayerStateGateway state) {
        super(state);
    }

    @Override
    public ActionType type() {
        return ActionType.BUILD_ROBOT_FACTORY;
    }
}
