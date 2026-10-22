package com.xmas.game.actions.service;

import com.xmas.game.actions.config.ActionProperties;
import com.xmas.game.actions.domain.ActionCategory;
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
                .requiresBuilt(cfg.getRequiresBuilt())
                .maxLevel(type.getCategory() == ActionCategory.RESEARCH ? Math.max(1, cfg.getMaxLevel()) : 1)
                .levelCosts(type.getCategory() == ActionCategory.RESEARCH
                        ? perLevel(cfg.getCostCash(), cfg.getMaxLevel(), properties.getResearchLevelCostFactors())
                        : List.of())
                .levelDurationsMillis(type.getCategory() == ActionCategory.RESEARCH
                        ? perLevel(cfg.getDurationMillis(), cfg.getMaxLevel(), properties.getResearchLevelDurationFactors())
                        : List.of())
                .build()));
        log.info("Loaded {} action definitions", definitions.size());
    }

    /** base x factor for each level; levels past the factor list reuse its last factor. */
    private static List<Long> perLevel(long base, int maxLevel, List<Double> factors) {
        List<Long> values = new ArrayList<>();
        for (int level = 1; level <= Math.max(1, maxLevel); level++) {
            double factor = factors.isEmpty() ? 1.0 : factors.get(Math.min(level, factors.size()) - 1);
            values.add(Math.round(base * factor));
        }
        return values;
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
