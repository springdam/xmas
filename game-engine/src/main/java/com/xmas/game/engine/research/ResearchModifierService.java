package com.xmas.game.engine.research;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.config.ResearchProperties;
import com.xmas.game.engine.entity.PlayerResearchEntity;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a player's research levels into the multipliers the game applies. Level 0 = not researched. */
@Service
@RequiredArgsConstructor
public class ResearchModifierService {

    private final PlayerResearchRepository repository;
    private final ResearchProperties properties;

    @Transactional(readOnly = true)
    public double economyMultiplier(UUID playerId) {
        return value(properties.getEconomyMultiplier(), level(playerId, ActionType.RESEARCH_ADVANCED_FARMING), 1.0);
    }

    @Transactional(readOnly = true)
    public double combatMultiplier(UUID playerId) {
        return value(properties.getCombatMultiplier(), level(playerId, ActionType.RESEARCH_ADVANCED_WEAPONRY), 1.0);
    }

    @Transactional(readOnly = true)
    public double espionageMultiplier(UUID playerId) {
        return value(properties.getEspionageMultiplier(), level(playerId, ActionType.RESEARCH_CRYPTOGRAPHY), 1.0);
    }

    @Transactional(readOnly = true)
    public double coupChanceMultiplier(UUID playerId) {
        return value(properties.getCoupChanceMultiplier(),
                level(playerId, ActionType.RESEARCH_CONTINUITY_OF_GOVERNMENT), 1.0);
    }

    @Transactional(readOnly = true)
    public double factoryIncomeMultiplier(UUID playerId) {
        return value(properties.getIndustrialAutomationMultiplier(),
                level(playerId, ActionType.RESEARCH_INDUSTRIAL_AUTOMATION), 1.0);
    }

    @Transactional(readOnly = true)
    public double bankIncomeMultiplier(UUID playerId) {
        return value(properties.getCentralBankingMultiplier(),
                level(playerId, ActionType.RESEARCH_CENTRAL_BANKING), 1.0);
    }

    @Transactional(readOnly = true)
    public double warEconomyMultiplier(UUID playerId, boolean atWar) {
        return atWar
                ? value(properties.getWarEconomyMultiplier(), level(playerId, ActionType.RESEARCH_WAR_ECONOMY), 1.0)
                : 1.0;
    }

    @Transactional(readOnly = true)
    public double combatDefenseMultiplier(UUID playerId) {
        return value(properties.getFortificationsMultiplier(),
                level(playerId, ActionType.RESEARCH_FORTIFICATIONS), 1.0);
    }

    @Transactional(readOnly = true)
    public double airSuperiorityMultiplier(UUID playerId) {
        return value(properties.getAirSuperiorityMultiplier(),
                level(playerId, ActionType.RESEARCH_AIR_SUPERIORITY), 1.0);
    }

    @Transactional(readOnly = true)
    public int warheadMultiplier(UUID playerId) {
        return value(properties.getMirvWarheadMultiplier(), level(playerId, ActionType.RESEARCH_MIRV), 1);
    }

    @Transactional(readOnly = true)
    public double counterIntelMultiplier(UUID playerId) {
        return value(properties.getCounterIntelMultiplier(),
                level(playerId, ActionType.RESEARCH_COUNTER_INTELLIGENCE_DOCTRINE), 1.0);
    }

    @Transactional(readOnly = true)
    public double protectionDefenseMultiplier(UUID playerId) {
        return value(properties.getPraetorianGuardMultiplier(),
                level(playerId, ActionType.RESEARCH_PRAETORIAN_GUARD), 1.0);
    }

    @Transactional(readOnly = true)
    public double paradePraiseDecay(UUID playerId, double baseDecay) {
        return value(properties.getMassMediaDecay(), level(playerId, ActionType.RESEARCH_MASS_MEDIA), baseDecay);
    }

    @Transactional(readOnly = true)
    public double popularityDamageMultiplier(UUID playerId) {
        return value(properties.getPropagandaDamageMultiplier(),
                level(playerId, ActionType.RESEARCH_PROPAGANDA_MINISTRY), 1.0);
    }

    /** The value for this level (levels beyond the list use its last value); {@code none} at level 0. */
    private static <T> T value(List<T> perLevel, int level, T none) {
        if (level <= 0 || perLevel.isEmpty()) {
            return none;
        }
        return perLevel.get(Math.min(level, perLevel.size()) - 1);
    }

    private int level(UUID playerId, ActionType research) {
        return repository.findByPlayerIdAndResearchType(playerId, research)
                .map(PlayerResearchEntity::getLevel)
                .orElse(0);
    }
}
