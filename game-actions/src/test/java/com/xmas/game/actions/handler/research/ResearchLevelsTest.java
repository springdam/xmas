package com.xmas.game.actions.handler.research;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.domain.ActionCategory;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResearchLevelsTest {

    private final UUID player = UUID.randomUUID();
    private final PlayerStateGateway state = mock(PlayerStateGateway.class);
    private final ResearchGateway research = mock(ResearchGateway.class);
    private final ResearchCryptographyHandler handler = new ResearchCryptographyHandler(state, research);

    private ActionContext ctx(int maxLevel) {
        return ActionContext.builder()
                .actorPlayerId(player)
                .quantity(1)
                .definition(ActionDefinition.builder()
                        .actionType(ActionType.RESEARCH_CRYPTOGRAPHY).category(ActionCategory.RESEARCH)
                        .costCash(90_000).durationMillis(300_000).maxLevel(maxLevel)
                        .levelCosts(List.of(90_000L, 180_000L, 270_000L))
                        .levelDurationsMillis(List.of(300_000L, 450_000L, 600_000L))
                        .build())
                .build();
    }

    @Test
    void theNextLevelCostsAndTakesMore() {
        when(state.getCash(player)).thenReturn(1_000_000L);
        when(state.pendingResearch(player)).thenReturn(Optional.empty());
        when(research.level(player, ActionType.RESEARCH_CRYPTOGRAPHY)).thenReturn(1);

        handler.validate(ctx(3));
        handler.begin(ctx(3));

        verify(state).charge(player, 180_000L);
        assertThat(handler.durationMillis(ctx(3))).isEqualTo(450_000L);
    }

    @Test
    void cannotGoPastTheTopLevel() {
        when(state.getCash(player)).thenReturn(1_000_000L);
        when(research.level(player, ActionType.RESEARCH_CRYPTOGRAPHY)).thenReturn(3);

        assertThatThrownBy(() -> handler.validate(ctx(3)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("highest level (3)");
    }

    @Test
    void aOneLevelUnlockCanOnlyBeResearchedOnce() {
        when(research.level(player, ActionType.RESEARCH_CRYPTOGRAPHY)).thenReturn(1);

        assertThatThrownBy(() -> handler.validate(ctx(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been researched");
    }

    @Test
    void completingNeverOvershootsTheTopLevel() {
        // e.g. the same level was stolen while this research was under way
        when(research.level(player, ActionType.RESEARCH_CRYPTOGRAPHY)).thenReturn(3);

        handler.onComplete(ctx(3));

        verify(research, never()).advanceResearch(player, ActionType.RESEARCH_CRYPTOGRAPHY);
    }
}
