package com.xmas.game.actions.handler.space;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.domain.ActionCategory;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.space.SpaceGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpaceProjectHandlerTest {

    private final UUID player = UUID.randomUUID();
    private final PlayerStateGateway state = mock(PlayerStateGateway.class);
    private final SpaceGateway space = mock(SpaceGateway.class);
    private final SpaceProjectHandler handler = new SpaceProjectHandler(ActionType.SPACE_SECOND_STAGE, state, space);

    private final ActionContext ctx = ActionContext.builder()
            .actorPlayerId(player)
            .quantity(1)
            .definition(ActionDefinition.builder()
                    .actionType(ActionType.SPACE_SECOND_STAGE).category(ActionCategory.SPACE)
                    .costCash(300_000).requiresBuilt(ActionType.SPACE_FIRST_STAGE).build())
            .build();

    @BeforeEach
    void setUp() {
        when(state.getCash(player)).thenReturn(1_000_000L);
        when(state.pendingIn(player, ActionCategory.SPACE)).thenReturn(Optional.empty());
    }

    @Test
    void needsThePreviousPartFirst() {
        assertThatThrownBy(() -> handler.validate(ctx))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("needs SPACE_FIRST_STAGE first");

        when(space.hasPart(player, ActionType.SPACE_FIRST_STAGE)).thenReturn(true);
        assertThatCode(() -> handler.validate(ctx)).doesNotThrowAnyException();
    }

    @Test
    void onePartUnderConstructionAtATime() {
        when(space.hasPart(player, ActionType.SPACE_FIRST_STAGE)).thenReturn(true);
        when(state.pendingIn(player, ActionCategory.SPACE)).thenReturn(Optional.of(ActionType.SPACE_PRIMARY_BOOSTERS));

        assertThatThrownBy(() -> handler.validate(ctx))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("still building SPACE_PRIMARY_BOOSTERS");
    }

    @Test
    void eachPartIsBuiltOnce() {
        when(space.hasPart(player, ActionType.SPACE_SECOND_STAGE)).thenReturn(true);

        assertThatThrownBy(() -> handler.validate(ctx))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("already built");
        handler.onComplete(ctx);
        verify(space, never()).addPart(player, ActionType.SPACE_SECOND_STAGE);
    }
}
