package com.xmas.game.engine.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** The palace: a trophy with no game effect. How upgrades are earned or bought, and how it gets damaged. */
@Getter
@Setter
@ConfigurationProperties(prefix = "game.palace")
public class PalaceProperties {

    /** Highest tier of each part (1 wood, 2 stone, 3 marble, 4 gold). */
    private int maxTier = 4;
    /** Cash price of buying each tier (index 0 = tier 1). */
    private List<Long> tierCosts = List.of(20_000L, 50_000L, 120_000L, 250_000L);

    /** A token for every this many attacks won. */
    private int victoriesPerToken = 3;
    /** A token for every popularStreakTicks spent at or above this popularity. */
    private int popularityThreshold = 75;
    private long popularStreakTicks = 120;
    /** A token for every this many ticks a nation stays in power. */
    private long tenureTicks = 360;
    /** Making peace earns a token only if the war lasted at least this long (so war/peace can't be farmed). */
    private long minWarMinutes = 10;

    /** Palace tiers lost per cause. A lost battle costs one tier with chance = share of forces lost. */
    private int sabotageDamage = 1;
    private int vandalismDamage = 2;
    private int damagePerLandedWarhead = 1;
}
