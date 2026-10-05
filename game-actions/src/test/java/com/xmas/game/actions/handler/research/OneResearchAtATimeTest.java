package com.xmas.game.actions.handler.research;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OneResearchAtATimeTest {

    private final UUID player = UUID.randomUUID();
    private final PlayerStateGateway state = mock(PlayerStateGateway.class);
    private final ResearchCryptographyHandler handler =
            new ResearchCryptographyHandler(state, mock(ResearchGateway.class));

    private final ActionContext ctx = ActionContext.builder()
            .actorPlayerId(player)
            .quantity(1)
            .definition(ActionDefinition.builder().actionType(ActionType.RESEARCH_CRYPTOGRAPHY).costCash(90_000).build())
            .build();

    @Test
    void anotherProjectUnderWayBlocksNewResearch() {
        when(state.getCash(player)).thenReturn(1_000_000L);
        when(state.pendingResearch(player)).thenReturn(Optional.empty());
        assertThatCode(() -> handler.validate(ctx)).doesNotThrowAnyException();

        when(state.pendingResearch(player)).thenReturn(Optional.of(ActionType.RESEARCH_MASS_MEDIA));
        assertThatThrownBy(() -> handler.validate(ctx))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("still working on RESEARCH_MASS_MEDIA");
    }

    @Test
    void theSameProjectCannotBeStartedTwice() {
        when(state.getCash(player)).thenReturn(1_000_000L);
        when(state.pendingResearch(player)).thenReturn(Optional.of(ActionType.RESEARCH_CRYPTOGRAPHY));

        assertThatThrownBy(() -> handler.validate(ctx)).isInstanceOf(BadRequestException.class);
    }
}
