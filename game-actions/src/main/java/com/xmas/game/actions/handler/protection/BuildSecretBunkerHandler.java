package com.xmas.game.actions.handler.protection;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractBuildHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class BuildSecretBunkerHandler extends AbstractBuildHandler {

    public BuildSecretBunkerHandler(PlayerStateGateway state) {
        super(state);
    }

    @Override
    public ActionType type() {
        return ActionType.BUILD_SECRET_BUNKER;
    }
}
