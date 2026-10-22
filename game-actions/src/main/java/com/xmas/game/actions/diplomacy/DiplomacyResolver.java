package com.xmas.game.actions.diplomacy;

import java.util.UUID;

/**
 * Seam for diplomatic actions. The engine implementation updates the relation
 * between the two players, applies popularity effects, transfers funds and
 * records news.
 */
public interface DiplomacyResolver {

    /** Whether the two players are currently at war with each other. */
    boolean atWar(UUID playerA, UUID playerB);

    void declareWar(UUID actorPlayerId, UUID targetPlayerId);

    void declarePeace(UUID actorPlayerId, UUID targetPlayerId);

    void sendMoney(UUID actorPlayerId, UUID targetPlayerId, long amount);

    /** Throws unless the actor may hold a parade now (cooldown over, and it would still raise popularity). */
    void assertCanHoldParade(UUID actorPlayerId);

    void holdParade(UUID actorPlayerId);

    void praiseLeader(UUID actorPlayerId, UUID targetPlayerId);

    void holdElection(UUID actorPlayerId);
}
