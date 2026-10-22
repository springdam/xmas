package com.xmas.game.actions.espionage;

import java.util.UUID;

/**
 * Seam for covert espionage operations. The engine implementation weighs the
 * actor's spies against the target's counter-intelligence, applies the effect
 * on success and records a covert (involved-only) news event.
 */
public interface EspionageResolver {

    void performHeist(UUID actorPlayerId, UUID targetPlayerId);

    void performSabotage(UUID actorPlayerId, UUID targetPlayerId);

    void performCyberAttack(UUID actorPlayerId, UUID targetPlayerId);

    void stealTechnology(UUID actorPlayerId, UUID targetPlayerId);

    void gatherIntelligence(UUID actorPlayerId, UUID targetPlayerId);

    void assassinateLeader(UUID actorPlayerId, UUID targetPlayerId);

    void vandalizePalace(UUID actorPlayerId, UUID targetPlayerId);
}
