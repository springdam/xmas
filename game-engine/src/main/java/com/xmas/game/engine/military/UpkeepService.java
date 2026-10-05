package com.xmas.game.engine.military;

import com.xmas.game.engine.config.UpkeepProperties;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

/**
 * Army upkeep: every tick a nation pays for its infantry, tanks, stealth bombers and special
 * ops. Defenses (batteries), spies and bodyguards are free. A nation that can't pay loses a
 * share of each unit type to desertion.
 */
@Service
@RequiredArgsConstructor
public class UpkeepService {

    private final UpkeepProperties properties;

    /** Units that walked off in one unpaid tick. */
    @Value
    public static class Deserters {
        int infantry;
        int tanks;
        int stealthBombers;
        int specialOps;

        public boolean any() {
            return infantry + tanks + stealthBombers + specialOps > 0;
        }
    }

    /** What the army costs per tick. */
    public long perTick(PlayerMilitaryEntity military) {
        return military.getInfantry() * properties.getInfantry()
                + military.getTanks() * properties.getTank()
                + military.getStealthBombers() * properties.getStealthBomber()
                + military.getSpecialOps() * properties.getSpecialOps();
    }

    /** Removes the deserting share of each unit type (at least one of any type still standing). */
    public Deserters desert(PlayerMilitaryEntity military) {
        Deserters deserters = new Deserters(
                deserting(military.getInfantry()), deserting(military.getTanks()),
                deserting(military.getStealthBombers()), deserting(military.getSpecialOps()));
        military.setInfantry(military.getInfantry() - deserters.getInfantry());
        military.setTanks(military.getTanks() - deserters.getTanks());
        military.setStealthBombers(military.getStealthBombers() - deserters.getStealthBombers());
        military.setSpecialOps(military.getSpecialOps() - deserters.getSpecialOps());
        return deserters;
    }

    private int deserting(int units) {
        return Math.min(units, (int) Math.ceil(units * properties.getDesertionFraction()));
    }
}
