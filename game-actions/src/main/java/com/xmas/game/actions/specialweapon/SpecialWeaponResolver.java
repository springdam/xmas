package com.xmas.game.actions.specialweapon;

import java.util.UUID;

/**
 * Seam for special-weapon strikes. The engine implementation runs each warhead
 * against the target's anti-missile interception, applies destruction for those
 * that land, consumes the launched warheads and records a public news event.
 */
public interface SpecialWeaponResolver {

    void launch(UUID actorPlayerId, UUID targetPlayerId, int warheads);
}
