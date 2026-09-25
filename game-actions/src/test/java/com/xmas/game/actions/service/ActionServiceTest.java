package com.xmas.game.actions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.xmas.game.actions.config.ActionProperties;
import com.xmas.game.actions.domain.ActionResult;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.economy.BuildFarmHandler;
import com.xmas.game.actions.queue.PendingActionQueue;
import com.xmas.game.actions.state.InMemoryPlayerStateGateway;
import com.xmas.game.common.error.BadRequestException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActionServiceTest {

    private InMemoryPlayerStateGateway state;
    private RecordingQueue queue;
    private ActionService actionService;
    private final UUID player = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ActionProperties properties = new ActionProperties();
        properties.getDefinitions().put(ActionType.BUILD_FARM, definition(10_000, 0, false));
        properties.getDefinitions().put(ActionType.PERFORM_CYBER_ATTACK, definition(5_000, 15_000, true));

        state = new InMemoryPlayerStateGateway();
        queue = new RecordingQueue();
        actionService = buildService(properties);
    }

    private ActionService buildService(ActionProperties properties) {
        ActionDefinitionCatalog catalog = new ActionDefinitionCatalog(properties);
        catalog.load();
        ActionHandlerRegistry registry = new ActionHandlerRegistry(List.of(new BuildFarmHandler(state)));
        return new ActionService(catalog, registry, queue, state, new AllResearchGateway());
    }

    private ActionProperties.Definition definition(long cost, long durationMillis, boolean targeted) {
        ActionProperties.Definition def = new ActionProperties.Definition();
        def.setCostCash(cost);
        def.setDurationMillis(durationMillis);
        def.setTargeted(targeted);
        return def;
    }

    @Test
    void buildFarm_immediatelyChargesAndGrantsAsset() {
        long before = state.getCash(player);

        ActionResult result = actionService.submit(ActionType.BUILD_FARM, player, null, 2);

        assertThat(result.isCompletedImmediately()).isTrue();
        assertThat(state.getCash(player)).isEqualTo(before - 20_000);
        assertThat(state.getAssetCount(player, ActionType.BUILD_FARM)).isEqualTo(2);
    }

    @Test
    void buildFarm_rejectsTargetOnNonTargetedAction() {
        assertThatThrownBy(() -> actionService.submit(ActionType.BUILD_FARM, player, UUID.randomUUID(), 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void buildFarm_rejectsInsufficientFunds() {
        assertThatThrownBy(() -> actionService.submit(ActionType.BUILD_FARM, player, null, 1_000))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void targetedAction_requiresTarget() {
        assertThatThrownBy(() -> actionService.submit(ActionType.PERFORM_CYBER_ATTACK, player, null, 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unimplementedAction_isRejected() {
        assertThatThrownBy(() ->
                actionService.submit(ActionType.PERFORM_CYBER_ATTACK, player, UUID.randomUUID(), 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void timedAction_chargesAndEnqueuesWithoutGrantingAsset() {
        ActionProperties properties = new ActionProperties();
        properties.getDefinitions().put(ActionType.BUILD_FARM, definition(10_000, 30_000, false));
        ActionService timedService = buildService(properties);
        long before = state.getCash(player);

        timedService.submit(ActionType.BUILD_FARM, player, null, 1);

        assertThat(state.getCash(player)).isEqualTo(before - 10_000);
        assertThat(state.getAssetCount(player, ActionType.BUILD_FARM)).isZero();
        assertThat(queue.calls).hasSize(1);
    }

    private static final class AllResearchGateway implements com.xmas.game.actions.research.ResearchGateway {
        @Override
        public boolean hasResearch(UUID playerId, ActionType research) {
            return true;
        }

        @Override
        public void addResearch(UUID playerId, ActionType research) {
        }
    }

    private static final class RecordingQueue implements PendingActionQueue {
        private final List<ActionType> calls = new ArrayList<>();

        @Override
        public void enqueue(ActionType type, UUID actor, UUID target, int quantity, Instant completesAt) {
            calls.add(type);
        }
    }
}
