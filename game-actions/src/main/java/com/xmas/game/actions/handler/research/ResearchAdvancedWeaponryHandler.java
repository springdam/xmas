package com.xmas.game.actions.handler.research;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

@Component
public class ResearchAdvancedWeaponryHandler extends AbstractResearchHandler {

    public ResearchAdvancedWeaponryHandler(PlayerStateGateway state, ResearchGateway research) {
        super(state, research);
    }

    @Override
    public ActionType type() {
        return ActionType.RESEARCH_ADVANCED_WEAPONRY;
    }
}
