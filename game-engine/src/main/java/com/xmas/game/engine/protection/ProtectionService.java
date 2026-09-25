package com.xmas.game.engine.protection;

import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.config.EspionageProperties;
import com.xmas.game.engine.entity.PlayerProtectionEntity;
import com.xmas.game.engine.repository.PlayerProtectionRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProtectionService {

    private final PlayerProtectionRepository protectionRepository;
    private final EspionageProperties properties;
    private final ResearchModifierService researchModifierService;

    /**
     * Defensive score that lowers the chance a leader-kill attempt succeeds.
     * Doppelgangers are excluded here — they absorb an otherwise-successful kill instead.
     */
    @Transactional(readOnly = true)
    public double defenseScore(UUID playerId) {
        PlayerProtectionEntity p = protection(playerId);
        double base = p.getBodyguards() * properties.getBodyguardDefense()
                + p.getNinjaBodyguards() * properties.getNinjaBodyguardDefense()
                + p.getBunkers() * properties.getBunkerDefense();
        return base * researchModifierService.protectionDefenseMultiplier(playerId);
    }

    @Transactional(readOnly = true)
    public int bodyguards(UUID playerId) {
        return protection(playerId).getBodyguards() + protection(playerId).getNinjaBodyguards();
    }

    private PlayerProtectionEntity protection(UUID playerId) {
        return protectionRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player protection not found: " + playerId));
    }
}
