package com.xmas.game.actions.service;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.common.error.BadRequestException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ActionHandlerRegistry {

    private final Map<ActionType, ActionHandler> handlers = new EnumMap<>(ActionType.class);

    public ActionHandlerRegistry(List<ActionHandler> discovered) {
        for (ActionHandler handler : discovered) {
            ActionHandler previous = handlers.put(handler.type(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate handler for " + handler.type());
            }
        }
        log.info("Registered {} action handlers", handlers.size());
    }

    public ActionHandler get(ActionType type) {
        ActionHandler handler = handlers.get(type);
        if (handler == null) {
            throw new BadRequestException("Action not implemented: " + type);
        }
        return handler;
    }
}
