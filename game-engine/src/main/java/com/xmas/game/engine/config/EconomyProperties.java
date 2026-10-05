package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.economy")
public class EconomyProperties {

    private long baseIncome = 1000;
    private long farmYield = 500;
    private long factoryYield = 1500;
    private long robotFactoryYield = 4000;
    private long bankYield = 2500;

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
    private double farmFragility = 0.25;
    private double factoryFragility = 0.6;
    private double bankFragility = 0.8;
    private double robotFactoryFragility = 1.0;
}
