package com.xmas.game.engine.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Research modifiers, one value per level (index 0 = level 1). Techs that only unlock
 * something (nuclear program, stealth, special forces, satellite network) have no entry here.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "game.research")
public class ResearchProperties {

    /** RESEARCH_ADVANCED_FARMING: income multiplier. */
    private List<Double> economyMultiplier = List.of(1.10, 1.20, 1.35);

    /** RESEARCH_ADVANCED_WEAPONRY: attack-power multiplier. */
    private List<Double> combatMultiplier = List.of(1.15, 1.30, 1.50);

    /** RESEARCH_CRYPTOGRAPHY: espionage strength multiplier. */
    private List<Double> espionageMultiplier = List.of(1.3, 1.6, 2.0);

    /** RESEARCH_CONTINUITY_OF_GOVERNMENT: coup-chance multiplier (lower = safer). */
    private List<Double> coupChanceMultiplier = List.of(0.6, 0.4, 0.2);

    /** RESEARCH_INDUSTRIAL_AUTOMATION: factory + robot-factory income multiplier. */
    private List<Double> industrialAutomationMultiplier = List.of(1.2, 1.4, 1.7);
    /** RESEARCH_CENTRAL_BANKING: bank income multiplier. */
    private List<Double> centralBankingMultiplier = List.of(1.2, 1.4, 1.7);
    /** RESEARCH_WAR_ECONOMY: income multiplier while at war with someone. */
    private List<Double> warEconomyMultiplier = List.of(1.15, 1.30, 1.50);
    /** RESEARCH_FORTIFICATIONS: defender combat-power multiplier. */
    private List<Double> fortificationsMultiplier = List.of(1.2, 1.4, 1.7);
    /** RESEARCH_AIR_SUPERIORITY: decapitation strike power multiplier from stealth bombers. */
    private List<Double> airSuperiorityMultiplier = List.of(1.3, 1.6, 2.0);
    /** RESEARCH_MIRV: each launched warhead counts as this many. */
    private List<Integer> mirvWarheadMultiplier = List.of(2, 3, 4);
    /** RESEARCH_COUNTER_INTELLIGENCE_DOCTRINE: counter-intel effectiveness multiplier on defense. */
    private List<Double> counterIntelMultiplier = List.of(1.3, 1.6, 2.0);
    /** RESEARCH_PRAETORIAN_GUARD: leadership protection (defense score) multiplier. */
    private List<Double> praetorianGuardMultiplier = List.of(1.3, 1.6, 2.0);
    /** RESEARCH_MASS_MEDIA: parade/praise decay used instead of the base (higher = slower falloff). */
    private List<Double> massMediaDecay = List.of(0.75, 0.85, 0.92);
    /** RESEARCH_PROPAGANDA_MINISTRY: fraction of popularity damage you take from enemy actions. */
    private List<Double> propagandaDamageMultiplier = List.of(0.75, 0.50, 0.30);
}
