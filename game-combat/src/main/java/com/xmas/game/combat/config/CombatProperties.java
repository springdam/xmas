package com.xmas.game.combat.config;

import com.xmas.game.combat.domain.UnitType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.combat")
public class CombatProperties {

    private double infantryAttack = 1.0;
    private double tankAttack = 4.0;
    private double stealthBomberAttack = 9.0;
    private double specialOpsAttack = 6.0;

    private double infantryDefense = 1.2;
    private double tankDefense = 3.0;
    private double stealthBomberDefense = 2.0;
    private double specialOpsDefense = 2.5;

    /** Multiplier a unit deals against the type it counters, and against the type that counters it. */
    private double counterBonus = 1.5;
    private double counterPenalty = 0.7;

    /** Scales how many units are destroyed relative to the power balance. */
    private double casualtyScale = 0.6;

    /** Smoothing constant for a decapitation strike's success against leadership protection. */
    private double decapitationResistance = 5.0;

    public double attack(UnitType type) {
        return switch (type) {
            case INFANTRY -> infantryAttack;
            case TANK -> tankAttack;
            case STEALTH_BOMBER -> stealthBomberAttack;
            case SPECIAL_OPS -> specialOpsAttack;
        };
    }

    public double defense(UnitType type) {
        return switch (type) {
            case INFANTRY -> infantryDefense;
            case TANK -> tankDefense;
            case STEALTH_BOMBER -> stealthBomberDefense;
            case SPECIAL_OPS -> specialOpsDefense;
        };
    }
}
