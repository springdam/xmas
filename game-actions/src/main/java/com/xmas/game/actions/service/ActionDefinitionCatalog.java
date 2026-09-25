package com.xmas.game.actions.service;

import com.xmas.game.actions.config.ActionProperties;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.common.error.BadRequestException;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionDefinitionCatalog {

    private final ActionProperties properties;
    private final Map<ActionType, ActionDefinition> definitions = new EnumMap<>(ActionType.class);

    @PostConstruct
    void load() {
        properties.getDefinitions().forEach((type, cfg) -> definitions.put(type, ActionDefinition.builder()
                .actionType(type)
                .category(type.getCategory())
                .costCash(cfg.getCostCash())
                .durationMillis(cfg.getDurationMillis())
                .targeted(cfg.isTargeted())
                .publicVisibility(cfg.isPublicVisibility())
                .description(cfg.getDescription())
                .maxTotal(cfg.getMaxTotal())
                .requiredResearch(cfg.getRequiredResearch())
                .build()));
        log.info("Loaded {} action definitions", definitions.size());
    }

    public ActionDefinition getDefinition(ActionType type) {
        ActionDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new BadRequestException("Action not available: " + type);
        }
        return definition;
    }

    public List<ActionDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }
}
