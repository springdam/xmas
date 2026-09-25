package com.xmas.game.engine.research;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.config.ResearchProperties;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResearchModifierService {

    private final PlayerResearchRepository repository;
    private final ResearchProperties properties;

    @Transactional(readOnly = true)
    public double economyMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_ADVANCED_FARMING) ? properties.getEconomyMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double combatMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_ADVANCED_WEAPONRY) ? properties.getCombatMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double espionageMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_CRYPTOGRAPHY) ? properties.getEspionageMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double coupChanceMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_CONTINUITY_OF_GOVERNMENT)
                ? properties.getCoupChanceMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double factoryIncomeMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_INDUSTRIAL_AUTOMATION)
                ? properties.getIndustrialAutomationMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double bankIncomeMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_CENTRAL_BANKING) ? properties.getCentralBankingMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double warEconomyMultiplier(UUID playerId, boolean atWar) {
        return atWar && has(playerId, ActionType.RESEARCH_WAR_ECONOMY) ? properties.getWarEconomyMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double combatDefenseMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_FORTIFICATIONS) ? properties.getFortificationsMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double airSuperiorityMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_AIR_SUPERIORITY) ? properties.getAirSuperiorityMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public int warheadMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_MIRV) ? properties.getMirvWarheadMultiplier() : 1;
    }

    @Transactional(readOnly = true)
    public double counterIntelMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_COUNTER_INTELLIGENCE_DOCTRINE)
                ? properties.getCounterIntelMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double protectionDefenseMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_PRAETORIAN_GUARD) ? properties.getPraetorianGuardMultiplier() : 1.0;
    }

    @Transactional(readOnly = true)
    public double paradePraiseDecay(UUID playerId, double baseDecay) {
        return has(playerId, ActionType.RESEARCH_MASS_MEDIA) ? properties.getMassMediaDecay() : baseDecay;
    }

    @Transactional(readOnly = true)
    public double popularityDamageMultiplier(UUID playerId) {
        return has(playerId, ActionType.RESEARCH_PROPAGANDA_MINISTRY)
                ? properties.getPropagandaDamageMultiplier() : 1.0;
    }

    private boolean has(UUID playerId, ActionType research) {
        return repository.existsByPlayerIdAndResearchType(playerId, research);
    }
}
