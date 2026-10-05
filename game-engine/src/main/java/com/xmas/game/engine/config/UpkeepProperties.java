package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Per-tick upkeep of standing army units (about 1% of their price), so armies can't grow without limit. */
@Getter
@Setter
@ConfigurationProperties(prefix = "game.upkeep")
public class UpkeepProperties {

    private long infantry = 20;
    private long tank = 80;
    private long stealthBomber = 250;
    private long specialOps = 200;

    /** Share of each unit type that deserts in a tick the nation can't pay its army. */
    private double desertionFraction = 0.1;
}
