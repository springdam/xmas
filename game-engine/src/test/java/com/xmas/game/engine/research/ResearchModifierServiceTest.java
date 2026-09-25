package com.xmas.game.engine.research;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.config.ResearchProperties;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResearchModifierServiceTest {

    private final UUID player = UUID.randomUUID();

    @Test
    void appliesMultiplierOnlyForResearchedTechs() {
        PlayerResearchRepository repository = mock(PlayerResearchRepository.class);
        when(repository.existsByPlayerIdAndResearchType(player, ActionType.RESEARCH_ADVANCED_FARMING))
                .thenReturn(true);
        when(repository.existsByPlayerIdAndResearchType(player, ActionType.RESEARCH_ADVANCED_WEAPONRY))
                .thenReturn(false);
        when(repository.existsByPlayerIdAndResearchType(player, ActionType.RESEARCH_CRYPTOGRAPHY))
                .thenReturn(false);

        ResearchModifierService service = new ResearchModifierService(repository, new ResearchProperties());

        assertThat(service.economyMultiplier(player)).isEqualTo(1.25);
        assertThat(service.combatMultiplier(player)).isEqualTo(1.0);
        assertThat(service.espionageMultiplier(player)).isEqualTo(1.0);
    }
}
