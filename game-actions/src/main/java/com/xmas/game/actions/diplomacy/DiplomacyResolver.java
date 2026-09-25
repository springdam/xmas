package com.xmas.game.actions.diplomacy;

import java.util.UUID;

/**
 * Seam for diplomatic actions. The engine implementation updates the relation
 * between the two players, applies popularity effects, transfers funds and
 * records news.
 */
public interface DiplomacyResolver {

    void declareWar(UUID actorPlayerId, UUID targetPlayerId);

    void declarePeace(UUID actorPlayerId, UUID targetPlayerId);

    void sendMoney(UUID actorPlayerId, UUID targetPlayerId, long amount);

    void holdParade(UUID actorPlayerId);

    void praiseLeader(UUID actorPlayerId, UUID targetPlayerId);

    void holdElection(UUID actorPlayerId);
}
