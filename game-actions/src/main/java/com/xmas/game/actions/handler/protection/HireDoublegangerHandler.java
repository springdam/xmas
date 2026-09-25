package com.xmas.game.actions.handler.protection;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractBuildHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class HireDoublegangerHandler extends AbstractBuildHandler {

    public HireDoublegangerHandler(PlayerStateGateway state) {
        super(state);
    }

    @Override
    public ActionType type() {
        return ActionType.HIRE_DOUBLEGANGER;
    }
}
