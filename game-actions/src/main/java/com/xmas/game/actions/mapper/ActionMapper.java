package com.xmas.game.actions.mapper;

import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionResult;
import com.xmas.game.actions.domain.ActionType;
import java.time.ZoneOffset;

public final class ActionMapper {

    private ActionMapper() {
    }

    public static com.xmas.game.actions.api.model.ActionDefinition toDto(ActionDefinition d) {
        return new com.xmas.game.actions.api.model.ActionDefinition()
                .actionType(com.xmas.game.actions.api.model.ActionType.fromValue(d.getActionType().name()))
                .category(com.xmas.game.actions.api.model.ActionCategory.fromValue(d.getCategory().name()))
                .costCash(d.getCostCash())
                .durationMillis(d.getDurationMillis())
                .targeted(d.isTargeted())
                .publicVisibility(d.isPublicVisibility())
                .description(d.getDescription())
                .maxTotal(d.getMaxTotal())
                .requiredResearch(d.getRequiredResearch() == null ? null
                        : com.xmas.game.actions.api.model.ActionType.fromValue(d.getRequiredResearch().name()))
                .requiresBuilt(d.getRequiresBuilt() == null ? null
                        : com.xmas.game.actions.api.model.ActionType.fromValue(d.getRequiresBuilt().name()))
                .maxLevel(d.getMaxLevel())
                .levelCosts(d.getLevelCosts())
                .levelDurationsMillis(d.getLevelDurationsMillis());
    }

    public static com.xmas.game.actions.api.model.ActionResult toDto(ActionResult r) {
        return new com.xmas.game.actions.api.model.ActionResult()
                .actionType(com.xmas.game.actions.api.model.ActionType.fromValue(r.getActionType().name()))
                .completedImmediately(r.isCompletedImmediately())
                .completesAt(r.getCompletesAt() == null ? null : r.getCompletesAt().atOffset(ZoneOffset.UTC))
                .message(r.getMessage());
    }

    public static ActionType toDomainType(com.xmas.game.actions.api.model.ActionType apiType) {
        return ActionType.valueOf(apiType.getValue());
    }
}
