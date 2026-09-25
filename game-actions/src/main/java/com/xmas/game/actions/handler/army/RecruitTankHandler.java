package com.xmas.game.actions.handler.army;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.AbstractBuildHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class RecruitTankHandler extends AbstractBuildHandler {

    public RecruitTankHandler(PlayerStateGateway state) {
        super(state);
    }

    @Override
    public ActionType type() {
        return ActionType.RECRUIT_TANK;
    }
}
