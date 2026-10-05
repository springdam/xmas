package com.xmas.game.actions.service;

import com.xmas.game.actions.domain.ActionCategory;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionResult;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.queue.PendingActionQueue;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {

    private final ActionDefinitionCatalog catalog;
    private final ActionHandlerRegistry registry;
    private final PendingActionQueue pendingActionQueue;
    private final PlayerStateGateway playerStateGateway;
    private final ResearchGateway researchGateway;

    public List<ActionDefinition> listDefinitions() {
        return catalog.all();
    }

    public ActionResult submit(ActionType type, UUID actorPlayerId, UUID targetPlayerId, int quantity) {
        playerStateGateway.assertCanAct(actorPlayerId);
        ActionDefinition definition = catalog.getDefinition(type);
        playerStateGateway.assertNotFrozen(actorPlayerId, isMilitary(type, definition));
        requireResearch(definition, actorPlayerId);
        validateTargeting(definition, actorPlayerId, targetPlayerId);

        ActionContext ctx = ActionContext.builder()
                .actorPlayerId(actorPlayerId)
                .targetPlayerId(targetPlayerId)
                .quantity(quantity)
                .definition(definition)
                .build();

        ActionHandler handler = registry.get(type);
        handler.validate(ctx);
        handler.begin(ctx);

        if (definition.getDurationMillis() <= 0) {
            handler.onComplete(ctx);
            log.debug("Action {} by {} completed immediately", type, actorPlayerId);
            return ActionResult.builder()
                    .actionType(type)
                    .completedImmediately(true)
                    .completesAt(Instant.now())
                    .message("Completed")
                    .build();
        }

        Instant completesAt = Instant.now().plusMillis(definition.getDurationMillis());
        pendingActionQueue.enqueue(type, actorPlayerId, targetPlayerId, quantity, completesAt);
        log.debug("Action {} by {} scheduled until {}", type, actorPlayerId, completesAt);
        return ActionResult.builder()
                .actionType(type)
                .completedImmediately(false)
                .completesAt(completesAt)
                .message("Scheduled")
                .build();
    }

    private void requireResearch(ActionDefinition definition, UUID actorPlayerId) {
        ActionType required = definition.getRequiredResearch();
        if (required != null && !researchGateway.hasResearch(actorPlayerId, required)) {
            throw new BadRequestException(definition.getActionType() + " requires researching " + required + " first");
        }
    }

    private boolean isMilitary(ActionType type, ActionDefinition definition) {
        return definition.getCategory() == ActionCategory.ARMY
                || type == ActionType.ATTACK
                || type == ActionType.SPECIAL_WEAPON_ATTACK;
    }

    private void validateTargeting(ActionDefinition definition, UUID actorPlayerId, UUID targetPlayerId) {
        if (definition.isTargeted() && targetPlayerId == null) {
            throw new BadRequestException("Action " + definition.getActionType() + " requires a target player");
        }
        if (!definition.isTargeted() && targetPlayerId != null) {
            throw new BadRequestException("Action " + definition.getActionType() + " cannot target a player");
        }
        if (targetPlayerId != null) {
            playerStateGateway.assertValidTarget(actorPlayerId, targetPlayerId);
        }
    }
}
