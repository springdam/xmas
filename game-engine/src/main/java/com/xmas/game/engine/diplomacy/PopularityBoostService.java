package com.xmas.game.engine.diplomacy;

import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.research.ResearchModifierService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Popularity boosts (parades, praise, aid) wear off: each one is scaled by decay^boostCount,
 * where the beneficiary's boost count grows with every boost and slowly recovers in the tick.
 * Parades also have a cooldown between them.
 */
@Service
@RequiredArgsConstructor
public class PopularityBoostService {

    private final PopularityProperties properties;
    private final ResearchModifierService researchModifierService;

    /** A boost of {@code baseBonus} after {@code boostCount} earlier ones, at {@code decayOwner}'s decay. */
    public int gain(int baseBonus, int boostCount, UUID decayOwner) {
        double decay = researchModifierService.paradePraiseDecay(decayOwner, properties.getParadePraiseDecay());
        return (int) Math.round(baseBonus * Math.pow(decay, boostCount));
    }

    /** What the player's next parade would add to their popularity. */
    public int nextParadeGain(PlayerEntity player) {
        return gain(properties.getParadeBonus(), player.getPopularityBoostCount(), player.getId());
    }

    /** The first tick the player may hold a parade again (0 if they never held one). */
    public long paradeReadyAtTick(PlayerEntity player) {
        return player.getLastParadeTick() == null ? 0 : player.getLastParadeTick() + properties.getParadeCooldownTicks();
    }
}
