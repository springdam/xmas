package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.popularity")
public class PopularityProperties {

    /** Popularity runs 1..100; players begin here. */
    private int startingPopularity = 50;

    private int declareWarPenalty = 10;
    private int declarePeaceBonus = 8;
    /** Aid earns the sender sendMoneyBonus (diminishing like parades) only from this amount up. */
    private int sendMoneyBonus = 3;
    private long sendMoneyBonusMinAmount = 50_000;
    private int paradeBonus = 8;
    private int praiseBonus = 4;
    /** Each successive parade/praise on a player is multiplied by decay^boostCount until they hold an election. */
    private double paradePraiseDecay = 0.6;
    /** How many ticks a player is frozen (military actions only) after holding an election. */
    private long electionFreezeTicks = 12;

    // Popularity a target loses when their nation is hit (a weakened leader becomes vulnerable).
    private int nukeVictimPenalty = 15;
    // A nuclear strike destabilizes everyone: every player loses this, plus the aggressor loses extra.
    private int nukeGlobalDrop = 3;
    private int nukeAttackerPenalty = 10;
    private int attackVictimPenalty = 5;
    /** Each lost battle is multiplied by decay^warStrain; strain drops by one every recovery-ticks ticks. */
    private double attackPenaltyDecay = 0.7;
    private long warStrainRecoveryTicks = 12;
    private int sabotageVictimPenalty = 6;
    private int cyberVictimPenalty = 6;
    private int heistVictimPenalty = 3;

    /** Income is scaled by 1 + (popularity - 50) * this, clamped to [minIncomeFactor, maxIncomeFactor]. */
    private double incomePerPopularityPoint = 0.01;
    private double minIncomeFactor = 0.5;
    private double maxIncomeFactor = 1.5;

    /** Below this popularity a leader becomes vulnerable to assassination, decapitation and coup. */
    private int vulnerabilityThreshold = 20;

    /** Per-tick probability that a vulnerable (popularity &lt; threshold) leader is overthrown in a coup. */
    private double coupChancePerTick = 0.03;
}
