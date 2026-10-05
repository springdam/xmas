package com.xmas.game.engine.economy;

import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

/**
 * Destroys buildings when a nation is struck. Each building rolls on its own: it is destroyed
 * with chance strength × its fragility, so productive buildings (robot factories, banks) break
 * far more easily than farms.
 */
@Service
@RequiredArgsConstructor
public class BuildingDamageService {

    private final EconomyProperties properties;
    private final RandomSource random;

    /** Buildings destroyed by one strike. */
    @Value
    public static class Losses {
        int farms;
        int factories;
        int robotFactories;
        int banks;
    }

    /** Rolls every building of the economy against the strike and removes the destroyed ones. */
    public Losses strike(PlayerEconomyEntity economy, double strength) {
        int farms = roll(economy.getFarms(), strength * properties.getFarmFragility());
        int factories = roll(economy.getFactories(), strength * properties.getFactoryFragility());
        int robotFactories = roll(economy.getRobotFactories(), strength * properties.getRobotFactoryFragility());
        int banks = roll(economy.getBanks(), strength * properties.getBankFragility());
        economy.setFarms(economy.getFarms() - farms);
        economy.setFactories(economy.getFactories() - factories);
        economy.setRobotFactories(economy.getRobotFactories() - robotFactories);
        economy.setBanks(economy.getBanks() - banks);
        return new Losses(farms, factories, robotFactories, banks);
    }

    /** "2 factories, 1 bank and 1 robot factory", or "no buildings". */
    public static String describe(Losses losses) {
        List<String> parts = new ArrayList<>();
        add(parts, losses.getFarms(), "farm", "farms");
        add(parts, losses.getFactories(), "factory", "factories");
        add(parts, losses.getBanks(), "bank", "banks");
        add(parts, losses.getRobotFactories(), "robot factory", "robot factories");
        if (parts.isEmpty()) {
            return "no buildings";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }

    private int roll(int count, double chance) {
        double p = Math.min(1.0, chance);
        int destroyed = 0;
        for (int i = 0; i < count; i++) {
            if (random.nextDouble() < p) {
                destroyed++;
            }
        }
        return destroyed;
    }

    private static void add(List<String> parts, int n, String one, String many) {
        if (n > 0) {
            parts.add(n + " " + (n == 1 ? one : many));
        }
    }
}
