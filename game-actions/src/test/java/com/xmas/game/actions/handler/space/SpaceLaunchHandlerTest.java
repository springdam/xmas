package com.xmas.game.actions.handler.space;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.domain.ActionCategory;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.space.SpaceGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpaceLaunchHandlerTest {

    private final UUID player = UUID.randomUUID();
    private final PlayerStateGateway state = mock(PlayerStateGateway.class);
    private final SpaceGateway space = mock(SpaceGateway.class);
    private final SpaceLaunchHandler handler = new SpaceLaunchHandler(state, space);

    private final ActionContext ctx = ActionContext.builder()
            .actorPlayerId(player)
            .quantity(1)
            .definition(ActionDefinition.builder()
                    .actionType(ActionType.SPACE_LAUNCH).category(ActionCategory.SPACE).costCash(250_000).build())
            .build();

    @Test
    void onlyAFinishedShipCanLaunch() {
        when(state.getCash(player)).thenReturn(1_000_000L);
        assertThatThrownBy(() -> handler.validate(ctx))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("isn't finished");

        when(space.shipComplete(player)).thenReturn(true);
        assertThatCode(() -> handler.validate(ctx)).doesNotThrowAnyException();
    }

    @Test
    void launchingWinsTheGame() {
        handler.onComplete(ctx);

        verify(space).launch(player);
    }
}
