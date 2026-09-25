package com.xmas.game.actions.research;

import com.xmas.game.actions.domain.ActionType;
import java.util.UUID;

/**
 * Seam for permanent research progress. The engine implementation persists
 * completed research and the modifier service reads it to adjust action effects.
 */
public interface ResearchGateway {

    boolean hasResearch(UUID playerId, ActionType research);

    void addResearch(UUID playerId, ActionType research);
}
