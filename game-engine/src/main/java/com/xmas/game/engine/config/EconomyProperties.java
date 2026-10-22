package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.economy")
public class EconomyProperties {

    private long baseIncome = 125;
    private long farmYield = 60;
    private long factoryYield = 190;
    private long robotFactoryYield = 500;
    private long bankYield = 310;

    /**
     * Food: each farm feeds this many factories/robot factories (robot factories first).
     * Industry beyond what the farms feed earns only unfedIndustryFactor of its yield.
     */
    private int industryPerFarm = 3;
    private double unfedIndustryFactor = 0.5;

    /** Factories and robot factories the given farms can feed at full output. */
    public int industryCapacity(int farms) {
        return farms * industryPerFarm;
    }

    /**
     * How easily each building is destroyed when struck (cyber attack, nuke): a building is
     * lost with chance strikeStrength * fragility. Less productive buildings are sturdier.
     */
    private double farmFragility = 0.05;
    private double factoryFragility = 0.6;
    private double bankFragility = 0.8;
    private double robotFactoryFragility = 1.0;

    /**
     * A defender who loses a battle has their buildings struck with strength
     * (share of their forces destroyed) x this, so heavier defeats wreck more of the economy.
     */
    private double battleDamageScale = 0.3;
}
