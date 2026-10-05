package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.espionage")
public class EspionageProperties {

    /** Smoothing constant so a defender with zero counter-intel is not automatically breached. */
    private double baseResistance = 2.0;

    /** Fraction of the target's cash stolen on a successful heist. */
    private double heistCashFraction = 0.2;

    /** Fraction of the target's military destroyed on a successful sabotage. */
    private double sabotageFraction = 0.15;

    /** Fraction of the target's industry destroyed on a successful cyber attack. */
    private double cyberFraction = 0.2;

    /** Fraction of the target's cash lost when their leader is assassinated. */
    private double assassinationCashPenalty = 0.25;

    /** Defensive weight each protection asset adds against assassination. */
    private double bodyguardDefense = 2.0;
    private double ninjaBodyguardDefense = 5.0;
    private double doublegangerDefense = 3.0;
    private double bunkerDefense = 2.0;

    /** On a failed operation, each spy is caught with chance spyCaptureRate * defense / (attack + defense). */
    private double spyCaptureRate = 0.5;
    /** Recon: range half-width fraction at zero clarity (tighter as clarity rises to 1). */
    private double reconMaxUncertainty = 0.5;
}
