package com.xmas.game.actions.handler.research;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class ResearchAirSuperiorityHandler extends AbstractResearchHandler {

    public ResearchAirSuperiorityHandler(PlayerStateGateway state, ResearchGateway research) {
        super(state, research);
    }

    @Override
    public ActionType type() {
        return ActionType.RESEARCH_AIR_SUPERIORITY;
    }
}
