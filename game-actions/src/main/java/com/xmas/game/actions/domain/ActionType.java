package com.xmas.game.actions.domain;

import lombok.Getter;

@Getter
public enum ActionType {

    BUILD_FARM(ActionCategory.ECONOMY),
    BUILD_FACTORY(ActionCategory.ECONOMY),
    BUILD_ROBOT_FACTORY(ActionCategory.ECONOMY),
    BUILD_BANK(ActionCategory.ECONOMY),
    DEMOLISH_FARM(ActionCategory.ECONOMY),
    DEMOLISH_FACTORY(ActionCategory.ECONOMY),
    DEMOLISH_ROBOT_FACTORY(ActionCategory.ECONOMY),
    DEMOLISH_BANK(ActionCategory.ECONOMY),

    RECRUIT_INFANTRY(ActionCategory.ARMY),
    RECRUIT_TANK(ActionCategory.ARMY),
    BUILD_MISSILE_BATTERY(ActionCategory.ARMY),
    BUILD_AIR_DEFENSE_BATTERY(ActionCategory.ARMY),
    BUILD_STEALTH_BOMBER(ActionCategory.ARMY),
    RECRUIT_SPECIAL_OPS(ActionCategory.ARMY),

    BUILD_ESPIONAGE_SATELLITE(ActionCategory.SPECIAL_WEAPONS),
    BUILD_NUKE(ActionCategory.SPECIAL_WEAPONS),

    HIRE_SPIES(ActionCategory.ESPIONAGE),
    HIRE_COUNTER_INTEL(ActionCategory.ESPIONAGE),
    PERFORM_CYBER_ATTACK(ActionCategory.ESPIONAGE),
    PERFORM_SABOTAGE(ActionCategory.ESPIONAGE),
    PERFORM_HEIST(ActionCategory.ESPIONAGE),
    STEAL_TECHNOLOGY(ActionCategory.ESPIONAGE),
    GATHER_INTELLIGENCE(ActionCategory.ESPIONAGE),
    ASSASSINATE_LEADER(ActionCategory.ESPIONAGE),
    VANDALIZE_PALACE(ActionCategory.ESPIONAGE),

    HIRE_DOUBLEGANGER(ActionCategory.PERSONAL_PROTECTION),
    BUILD_SECRET_BUNKER(ActionCategory.PERSONAL_PROTECTION),
    HIRE_BODYGUARDS(ActionCategory.PERSONAL_PROTECTION),
    HIRE_NINJA_BODYGUARDS(ActionCategory.PERSONAL_PROTECTION),

    RESEARCH_ADVANCED_FARMING(ActionCategory.RESEARCH),
    RESEARCH_ADVANCED_WEAPONRY(ActionCategory.RESEARCH),
    RESEARCH_CRYPTOGRAPHY(ActionCategory.RESEARCH),
    RESEARCH_NUCLEAR_PROGRAM(ActionCategory.RESEARCH),
    RESEARCH_STEALTH_TECHNOLOGY(ActionCategory.RESEARCH),
    RESEARCH_SPECIAL_FORCES_TRAINING(ActionCategory.RESEARCH),
    RESEARCH_CONTINUITY_OF_GOVERNMENT(ActionCategory.RESEARCH),
    RESEARCH_SATELLITE_NETWORK(ActionCategory.RESEARCH),
    RESEARCH_INDUSTRIAL_AUTOMATION(ActionCategory.RESEARCH),
    RESEARCH_CENTRAL_BANKING(ActionCategory.RESEARCH),
    RESEARCH_WAR_ECONOMY(ActionCategory.RESEARCH),
    RESEARCH_FORTIFICATIONS(ActionCategory.RESEARCH),
    RESEARCH_AIR_SUPERIORITY(ActionCategory.RESEARCH),
    RESEARCH_MIRV(ActionCategory.RESEARCH),
    RESEARCH_COUNTER_INTELLIGENCE_DOCTRINE(ActionCategory.RESEARCH),
    RESEARCH_PRAETORIAN_GUARD(ActionCategory.RESEARCH),
    RESEARCH_MASS_MEDIA(ActionCategory.RESEARCH),
    RESEARCH_PROPAGANDA_MINISTRY(ActionCategory.RESEARCH),

    DECLARE_WAR(ActionCategory.DIPLOMACY),
    DECLARE_PEACE(ActionCategory.DIPLOMACY),
    SEND_MONEY(ActionCategory.DIPLOMACY),
    HOLD_PARADE(ActionCategory.DIPLOMACY),
    PRAISE_LEADER(ActionCategory.DIPLOMACY),
    HOLD_ELECTION(ActionCategory.DIPLOMACY),
    ATTACK(ActionCategory.DIPLOMACY),
    SPECIAL_WEAPON_ATTACK(ActionCategory.DIPLOMACY),
    DECAPITATION_STRIKE(ActionCategory.ARMY);

    private final ActionCategory category;

    ActionType(ActionCategory category) {
        this.category = category;
    }

    /** Building an economy building takes a plot of the nation's land (demolishing frees one). */
    public boolean usesLand() {
        return this == BUILD_FARM || this == BUILD_FACTORY || this == BUILD_ROBOT_FACTORY || this == BUILD_BANK;
    }
}
