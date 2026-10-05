package com.xmas.game.actions.combat;

import java.util.UUID;

/**
 * Seam for conventional combat. The engine implementation loads both sides'
 * military, runs the probabilistic calculator, persists losses and emits news.
 */
public interface CombatResolver {

    /** Publicly announces that an attack has been launched and is on its way. */
    void announceAttack(UUID attackerPlayerId, UUID defenderPlayerId);

    void resolveAttack(UUID attackerPlayerId, UUID defenderPlayerId, int quantity);

    void resolveDecapitationStrike(UUID attackerPlayerId, UUID targetPlayerId);
}
