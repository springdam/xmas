package com.xmas.game.actions.space;

import com.xmas.game.actions.domain.ActionType;
import java.util.UUID;

/**
 * Seam for the Mars mission: which spaceship parts a player has built. The engine keeps them
 * private to the player (no public news, not visible to rivals or satellites).
 */
public interface SpaceGateway {

    boolean hasPart(UUID playerId, ActionType part);

    void addPart(UUID playerId, ActionType part);

    /** Whether every part of the ship is built. */
    boolean shipComplete(UUID playerId);

    /** Launches the ship: the player wins and the game ends. */
    void launch(UUID playerId);
}
