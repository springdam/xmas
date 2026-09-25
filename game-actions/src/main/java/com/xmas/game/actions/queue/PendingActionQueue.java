package com.xmas.game.actions.queue;

import com.xmas.game.actions.domain.ActionType;
import java.time.Instant;
import java.util.UUID;

/**
 * Seam for scheduling timed actions. The engine module persists these and the
 * game tick resolves them (calling the handler's onComplete) when completesAt elapses.
 */
public interface PendingActionQueue {

    void enqueue(ActionType type, UUID actorPlayerId, UUID targetPlayerId, int quantity, Instant completesAt);
}
