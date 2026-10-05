package com.xmas.game.engine.economy;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.config.NationProperties;
import com.xmas.game.engine.domain.PendingActionStatus;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.repository.PendingActionRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Land: every farm, factory, robot factory and bank takes one plot, and each nation has a
 * fixed number of plots (game.nations.start.&lt;nation&gt;.land). Buildings under construction
 * already hold their plot, so queued batches can't overbuild.
 */
@Service
@RequiredArgsConstructor
public class LandService {

    private static final List<ActionType> LAND_BUILDS =
            Arrays.stream(ActionType.values()).filter(ActionType::usesLand).toList();

    private final PlayerRepository playerRepository;
    private final PlayerEconomyRepository economyRepository;
    private final PendingActionRepository pendingActionRepository;
    private final NationProperties nationProperties;

    /** Plots the player's nation has; 0 for the admin, who has no nation. */
    @Transactional(readOnly = true)
    public int capacity(UUID playerId) {
        return playerRepository.findById(playerId)
                .filter(player -> player.getNation() != null)
                .map(player -> nationProperties.getStart().get(player.getNation()))
                .map(NationProperties.Start::getLand)
                .orElse(0);
    }

    /** Plots taken: standing buildings plus those under construction. */
    @Transactional(readOnly = true)
    public int inUse(UUID playerId) {
        int standing = economyRepository.findById(playerId)
                .map(LandService::buildings)
                .orElse(0);
        long building = pendingActionRepository.sumPendingQuantity(playerId, PendingActionStatus.PENDING, LAND_BUILDS);
        return standing + (int) building;
    }

    public int free(UUID playerId) {
        return Math.max(0, capacity(playerId) - inUse(playerId));
    }

    private static int buildings(PlayerEconomyEntity economy) {
        return economy.getFarms() + economy.getFactories() + economy.getRobotFactories() + economy.getBanks();
    }
}
