package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.special-forces")
public class SpecialForcesProperties {

    /** Decapitation strike power contributed per (air-defense-surviving) stealth bomber. */
    private double bomberDecapWeight = 4.0;
    /** Decapitation strike power per square root of ground units (infantry + tanks): 100 units give 5. */
    private double groundDecapWeight = 0.5;
    /** Stealth bombers neutralized per air-defense battery on a decapitation strike. */
    private int airDefenseNegationPerBattery = 2;
    /** Stealth bombers lost per defending bodyguard on a decapitation strike. */
    private double bomberLossPerBodyguard = 0.5;

    /** Assassination success (spy-equivalent) contributed per special-ops unit. */
    private double specialOpsAssassinationWeight = 3.0;
    /** Special-ops units lost per defending bodyguard on an assassination. */
    private double specialOpsLossPerBodyguard = 0.5;
}
