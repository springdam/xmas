package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Per-tick upkeep of standing army units (about 0.25% of their price), so armies can't grow without limit. */
@Getter
@Setter
@ConfigurationProperties(prefix = "game.upkeep")
public class UpkeepProperties {

    private long infantry = 5;
    private long tank = 20;
    private long stealthBomber = 60;
    private long specialOps = 50;

    /** Share of each unit type that deserts in a tick the nation can't pay its army. */
    private double desertionFraction = 0.1;
}
