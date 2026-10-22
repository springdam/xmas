package com.xmas.game.actions.handler.espionage;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.espionage.EspionageResolver;
import com.xmas.game.actions.handler.AbstractCovertActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.stereotype.Component;

/** Spies deface a rival's palace: pure humiliation, no effect on their economy or army. */
@Component
public class VandalizePalaceHandler extends AbstractCovertActionHandler {

    private final EspionageResolver espionageResolver;

    public VandalizePalaceHandler(PlayerStateGateway state, EspionageResolver espionageResolver) {
        super(state);
        this.espionageResolver = espionageResolver;
    }

    @Override
    public ActionType type() {
        return ActionType.VANDALIZE_PALACE;
    }

    @Override
    public void onComplete(ActionContext ctx) {
        espionageResolver.vandalizePalace(ctx.getActorPlayerId(), ctx.getTargetPlayerId());
    }
}
