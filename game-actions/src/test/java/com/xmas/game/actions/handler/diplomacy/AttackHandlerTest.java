package com.xmas.game.actions.handler.diplomacy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.combat.CombatResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttackHandlerTest {

    private final UUID actor = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final PlayerStateGateway state = mock(PlayerStateGateway.class);
    private final CombatResolver combat = mock(CombatResolver.class);
    private final AttackHandler handler = new AttackHandler(state, combat);

    private final ActionContext ctx = ActionContext.builder()
            .actorPlayerId(actor)
            .targetPlayerId(target)
            .quantity(1)
            .definition(ActionDefinition.builder().actionType(ActionType.ATTACK).costCash(15_000).targeted(true).build())
            .build();

    @Test
    void onlyOneAttackPerTargetAtATime() {
        when(state.getCash(actor)).thenReturn(1_000_000L);
        assertThatCode(() -> handler.validate(ctx)).doesNotThrowAnyException();

        when(state.hasPendingAgainst(actor, Set.of(ActionType.ATTACK), target)).thenReturn(true);
        assertThatThrownBy(() -> handler.validate(ctx))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("still under way");
    }

    @Test
    void launchingAnnouncesTheAttack() {
        handler.begin(ctx);

        verify(state).charge(actor, 15_000);
        verify(combat).announceAttack(actor, target);
    }
}
