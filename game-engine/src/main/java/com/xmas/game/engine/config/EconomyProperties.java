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
}
