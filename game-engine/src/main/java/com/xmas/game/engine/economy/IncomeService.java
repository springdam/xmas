package com.xmas.game.engine.economy;

import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.repository.PlayerRelationRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * A nation's income per tick: base income plus its buildings (industry only at full output
 * while farms feed it), scaled by research, popularity and War Economy.
 */
@Service
@RequiredArgsConstructor
public class IncomeService {

    private final EconomyProperties economyProperties;
    private final ResearchModifierService researchModifierService;
    private final PopularityProperties popularityProperties;
    private final PlayerRelationRepository relationRepository;

    private double popularityIncomeFactor(int popularity) {
        double factor = 1.0 + (popularity - 50) * popularityProperties.getIncomePerPopularityPoint();
        return Math.max(popularityProperties.getMinIncomeFactor(),
                Math.min(popularityProperties.getMaxIncomeFactor(), factor));
    }

    /** Cash the player's economy earns per tick, before army upkeep. */
    public long perTick(UUID playerId, PlayerEconomyEntity e, int popularity) {
        double farmIncome = (double) e.getFarms() * economyProperties.getFarmYield();
        // Farms feed industry; the most productive buildings get fed first.
        int food = economyProperties.industryCapacity(e.getFarms());
        int fedRobots = Math.min(e.getRobotFactories(), food);
        int fedFactories = Math.min(e.getFactories(), food - fedRobots);
        double factoryIncome = (fedOutput(fedFactories, e.getFactories()) * economyProperties.getFactoryYield()
                + fedOutput(fedRobots, e.getRobotFactories()) * economyProperties.getRobotFactoryYield())
                * researchModifierService.factoryIncomeMultiplier(playerId);
        double bankIncome = (double) e.getBanks() * economyProperties.getBankYield()
                * researchModifierService.bankIncomeMultiplier(playerId);
        double raw = economyProperties.getBaseIncome() + farmIncome + factoryIncome + bankIncome;
        double total = raw
                * researchModifierService.economyMultiplier(playerId)
                * popularityIncomeFactor(popularity)
                * researchModifierService.warEconomyMultiplier(playerId, relationRepository.isAtWar(playerId));
        return Math.round(total);
    }

    /** Effective building count: fed buildings in full, the rest at the unfed factor. */
    private double fedOutput(int fed, int total) {
        return fed + (total - fed) * economyProperties.getUnfedIndustryFactor();
    }
}
