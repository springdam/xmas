package com.xmas.game.actions.rest;

import com.xmas.game.actions.api.ActionsApi;
import com.xmas.game.actions.api.model.SubmitActionRequest;
import com.xmas.game.actions.domain.ActionResult;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.mapper.ActionMapper;
import com.xmas.game.actions.service.ActionService;
import com.xmas.game.common.security.SessionIdentity;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActionController implements ActionsApi {

    private final ActionService actionService;
    private final SessionIdentity sessionIdentity;

    @Override
    public ResponseEntity<List<com.xmas.game.actions.api.model.ActionDefinition>> listActions() {
        List<com.xmas.game.actions.api.model.ActionDefinition> body = actionService.listDefinitions()
                .stream()
                .map(ActionMapper::toDto)
                .toList();
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<com.xmas.game.actions.api.model.ActionResult> submitAction(
            String xSessionToken, SubmitActionRequest request) {
        UUID actorPlayerId = sessionIdentity.requirePlayer(xSessionToken, request.getGameId());
        ActionType type = ActionMapper.toDomainType(request.getActionType());
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        ActionResult result = actionService.submit(
                type, actorPlayerId, request.getTargetPlayerId(), quantity);
        return ResponseEntity.ok(ActionMapper.toDto(result));
    }
}
