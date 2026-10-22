package com.xmas.game.engine.research;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.config.ResearchProperties;
import com.xmas.game.engine.entity.PlayerResearchEntity;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResearchModifierServiceTest {

    private final UUID player = UUID.randomUUID();
    private final PlayerResearchRepository repository = mock(PlayerResearchRepository.class);
    private final ResearchModifierService service = new ResearchModifierService(repository, new ResearchProperties());

    private void researched(ActionType type, int level) {
        when(repository.findByPlayerIdAndResearchType(player, type)).thenReturn(Optional.of(
                PlayerResearchEntity.builder().playerId(player).researchType(type).level(level).build()));
    }

    @Test
    void unresearchedTechsChangeNothing() {
        when(repository.findByPlayerIdAndResearchType(eq(player), any())).thenReturn(Optional.empty());

        assertThat(service.economyMultiplier(player)).isEqualTo(1.0);
        assertThat(service.warheadMultiplier(player)).isEqualTo(1);
        assertThat(service.paradePraiseDecay(player, 0.6)).isEqualTo(0.6);
    }

    @Test
    void higherLevelsGiveBiggerBonuses() {
        researched(ActionType.RESEARCH_ADVANCED_FARMING, 1);
        assertThat(service.economyMultiplier(player)).isEqualTo(1.10);
        researched(ActionType.RESEARCH_ADVANCED_FARMING, 2);
        assertThat(service.economyMultiplier(player)).isEqualTo(1.20);
        researched(ActionType.RESEARCH_ADVANCED_FARMING, 3);
        assertThat(service.economyMultiplier(player)).isEqualTo(1.35);
    }

    @Test
    void protectiveTechsShrinkWhatYouSuffer() {
        researched(ActionType.RESEARCH_PROPAGANDA_MINISTRY, 3);
        researched(ActionType.RESEARCH_CONTINUITY_OF_GOVERNMENT, 2);
        researched(ActionType.RESEARCH_MIRV, 2);

        assertThat(service.popularityDamageMultiplier(player)).isEqualTo(0.30);
        assertThat(service.coupChanceMultiplier(player)).isEqualTo(0.4);
        assertThat(service.warheadMultiplier(player)).isEqualTo(3);
    }
}
