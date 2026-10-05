package com.xmas.game.actions.state;

import com.xmas.game.actions.domain.ActionType;
import java.util.UUID;

/**
 * Seam over player state. Phase 3 replaces the in-memory implementation with a
 * JPA-backed one operating on the per-player economy/military/etc. tables.
 */
public interface PlayerStateGateway {

    /** Throws if the player may not currently act (e.g. their government has been overthrown). */
    void assertCanAct(UUID playerId);

    /** Throws if the player is frozen (e.g. holding an election) and this is not a military action. */
    void assertNotFrozen(UUID playerId, boolean militaryAction);

    long getCash(UUID playerId);

    void charge(UUID playerId, long amount);

    void addAsset(UUID playerId, ActionType assetType, int quantity);

    int getAssetCount(UUID playerId, ActionType assetType);

    /** Land plots still free for economy buildings (counting those under construction as taken). */
    int getFreeLand(UUID playerId);
}
