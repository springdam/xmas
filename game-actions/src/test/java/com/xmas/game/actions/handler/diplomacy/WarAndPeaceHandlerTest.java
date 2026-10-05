package com.xmas.game.actions.handler.diplomacy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.diplomacy.DiplomacyResolver;
import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.state.InMemoryPlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WarAndPeaceHandlerTest {

    private final UUID actor = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final InMemoryPlayerStateGateway state = new InMemoryPlayerStateGateway();
    private final DiplomacyResolver resolver = mock(DiplomacyResolver.class);

    private ActionContext ctx(ActionType type) {
        return ActionContext.builder()
                .actorPlayerId(actor)
                .targetPlayerId(target)
                .quantity(1)
                .definition(ActionDefinition.builder().actionType(type).costCash(0).targeted(true).build())
                .build();
    }

    @Test
    void peace_requiresAWar() {
        DeclarePeaceHandler handler = new DeclarePeaceHandler(state, resolver);

        assertThatThrownBy(() -> handler.validate(ctx(ActionType.DECLARE_PEACE)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not at war");

        when(resolver.atWar(actor, target)).thenReturn(true);
        assertThatCode(() -> handler.validate(ctx(ActionType.DECLARE_PEACE))).doesNotThrowAnyException();
    }

    @Test
    void war_cannotBeDeclaredTwice() {
        DeclareWarHandler handler = new DeclareWarHandler(state, resolver);
        assertThatCode(() -> handler.validate(ctx(ActionType.DECLARE_WAR))).doesNotThrowAnyException();

        when(resolver.atWar(actor, target)).thenReturn(true);
        assertThatThrownBy(() -> handler.validate(ctx(ActionType.DECLARE_WAR)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already at war");
    }
}
