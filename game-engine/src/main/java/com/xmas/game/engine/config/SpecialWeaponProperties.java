package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.special-weapons")
public class SpecialWeaponProperties {

    /** Interception probability contributed per anti-missile battery. */
    private double interceptionPerBattery = 0.15;

    /** Cap on the combined interception probability. */
    private double maxInterceptionProbability = 0.9;

    /** Fraction of each of the target's asset pools destroyed per warhead that lands. */
    private double destructionPerWarhead = 0.35;
}
