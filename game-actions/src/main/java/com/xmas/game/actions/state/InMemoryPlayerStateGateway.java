package com.xmas.game.actions.state;

import com.xmas.game.actions.domain.ActionType;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory state store used by unit tests. The application wires the
 * JPA-backed implementation from the engine module.
 */
public class InMemoryPlayerStateGateway implements PlayerStateGateway {

    private static final long STARTING_CASH = 1_000_000L;

    private final Map<UUID, Long> cash = new ConcurrentHashMap<>();
    private final Map<UUID, Map<ActionType, Integer>> assets = new ConcurrentHashMap<>();

    @Override
    public void assertCanAct(UUID playerId) {
        // In-memory test gateway: all players may act.
    }

    @Override
    public void assertNotFrozen(UUID playerId, boolean militaryAction) {
        // In-memory test gateway: no freeze.
    }

    @Override
    public long getCash(UUID playerId) {
        return cash.computeIfAbsent(playerId, id -> STARTING_CASH);
    }

    @Override
    public void charge(UUID playerId, long amount) {
        cash.merge(playerId, -amount, Long::sum);
    }

    @Override
    public void addAsset(UUID playerId, ActionType assetType, int quantity) {
        assets.computeIfAbsent(playerId, id -> new EnumMap<>(ActionType.class))
                .merge(assetType, quantity, Integer::sum);
    }

    @Override
    public int getAssetCount(UUID playerId, ActionType assetType) {
        return assets.getOrDefault(playerId, Map.of()).getOrDefault(assetType, 0);
    }

    @Override
    public int getFreeLand(UUID playerId) {
        return Integer.MAX_VALUE;   // in-memory test gateway: unlimited land
    }

    @Override
    public boolean hasPendingAgainst(UUID playerId, ActionType type, UUID targetPlayerId) {
        return false;   // in-memory test gateway: no queue of its own
    }
}
