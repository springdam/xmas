package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.research")
public class ResearchProperties {

    /** Income multiplier granted by RESEARCH_ADVANCED_FARMING. */
    private double economyMultiplier = 1.25;

    /** Attack-power multiplier granted by RESEARCH_ADVANCED_WEAPONRY. */
    private double combatMultiplier = 1.25;

    /** Espionage success multiplier granted by RESEARCH_CRYPTOGRAPHY. */
    private double espionageMultiplier = 1.5;

    /** Coup-chance multiplier granted by RESEARCH_CONTINUITY_OF_GOVERNMENT (lower = safer). */
    private double coupChanceMultiplier = 0.4;

    /** RESEARCH_INDUSTRIAL_AUTOMATION: factory + robot-factory income multiplier. */
    private double industrialAutomationMultiplier = 1.4;
    /** RESEARCH_CENTRAL_BANKING: bank income multiplier. */
    private double centralBankingMultiplier = 1.4;
    /** RESEARCH_WAR_ECONOMY: income multiplier while at war with someone. */
    private double warEconomyMultiplier = 1.25;
    /** RESEARCH_FORTIFICATIONS: defender combat-power multiplier. */
    private double fortificationsMultiplier = 1.3;
    /** RESEARCH_AIR_SUPERIORITY: decapitation strike power multiplier from stealth bombers. */
    private double airSuperiorityMultiplier = 1.5;
    /** RESEARCH_MIRV: each launched warhead counts as this many. */
    private int mirvWarheadMultiplier = 2;
    /** RESEARCH_COUNTER_INTELLIGENCE_DOCTRINE: counter-intel effectiveness multiplier on defense. */
    private double counterIntelMultiplier = 1.5;
    /** RESEARCH_PRAETORIAN_GUARD: leadership protection (defense score) multiplier. */
    private double praetorianGuardMultiplier = 1.5;
    /** RESEARCH_MASS_MEDIA: parade/praise diminishing decay used instead of the base (higher = slower falloff). */
    private double massMediaDecay = 0.85;
    /** RESEARCH_PROPAGANDA_MINISTRY: fraction of popularity damage you take from enemy actions. */
    private double propagandaDamageMultiplier = 0.5;
}
