package com.xmas.game.actions.handler.espionage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.espionage.EspionageResolver;
import com.xmas.game.actions.handler.AbstractCovertActionHandler;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpyOperationSlotTest {

    private final UUID actor = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final PlayerStateGateway state = mock(PlayerStateGateway.class);
    private final PerformHeistHandler handler = new PerformHeistHandler(state, mock(EspionageResolver.class));

    private final ActionContext ctx = ActionContext.builder()
            .actorPlayerId(actor)
            .targetPlayerId(target)
            .quantity(1)
            .definition(ActionDefinition.builder().actionType(ActionType.PERFORM_HEIST).costCash(8_000).targeted(true).build())
            .build();

    @Test
    void anySpyOperationUnderWayBlocksAnotherAgainstTheSameNation() {
        when(state.getCash(actor)).thenReturn(1_000_000L);
        when(state.getAssetCount(actor, ActionType.HIRE_SPIES)).thenReturn(10);
        assertThatCode(() -> handler.validate(ctx)).doesNotThrowAnyException();

        when(state.hasPendingAgainst(actor, AbstractCovertActionHandler.SPY_OPERATIONS, target)).thenReturn(true);
        assertThatThrownBy(() -> handler.validate(ctx))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("still on an operation");
    }
}
