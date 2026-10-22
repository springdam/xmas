package com.xmas.game.actions.research;

import com.xmas.game.actions.domain.ActionType;
import java.util.UUID;

/**
 * Seam for permanent research progress. The engine implementation persists
 * completed research and the modifier service reads it to adjust action effects.
 */
public interface ResearchGateway {

    /** The player's level in this tech; 0 if not researched. */
    int level(UUID playerId, ActionType research);

    default boolean hasResearch(UUID playerId, ActionType research) {
        return level(playerId, research) > 0;
    }

    /** Raises the player's level in this tech by one (from 0 to 1 the first time). */
    void advanceResearch(UUID playerId, ActionType research);
}
