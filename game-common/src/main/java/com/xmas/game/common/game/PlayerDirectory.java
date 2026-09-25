package com.xmas.game.common.game;

import java.util.UUID;

/**
 * Looks up which game a player belongs to, for modules that don't own the player table.
 * Implemented in the application module over the engine's player repository.
 */
public interface PlayerDirectory {

    /** The player's game id, or throws 404 if the player doesn't exist. */
    UUID requireGameId(UUID playerId);
}
