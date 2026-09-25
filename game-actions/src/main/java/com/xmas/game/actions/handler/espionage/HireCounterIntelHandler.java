package com.xmas.game.actions.handler.espionage;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractBuildHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class HireCounterIntelHandler extends AbstractBuildHandler {

    public HireCounterIntelHandler(PlayerStateGateway state) {
        super(state);
    }

    @Override
    public ActionType type() {
        return ActionType.HIRE_COUNTER_INTEL;
    }
}
