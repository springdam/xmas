package com.xmas.game.engine.repository;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.domain.PendingActionStatus;
import com.xmas.game.engine.entity.PendingActionEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendingActionRepository extends JpaRepository<PendingActionEntity, UUID> {

    List<PendingActionEntity> findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(
            PendingActionStatus status, Instant threshold);

    boolean existsByActorPlayerIdAndActionTypeInAndTargetPlayerIdAndStatus(
            UUID actorPlayerId, Collection<ActionType> actionTypes, UUID targetPlayerId, PendingActionStatus status);

    /** Total quantity of this player's pending actions of the given types (e.g. buildings under construction). */
    @Query("""
            select coalesce(sum(p.quantity), 0) from PendingActionEntity p
            where p.actorPlayerId = :playerId and p.status = :status and p.actionType in :types
            """)
    long sumPendingQuantity(@Param("playerId") UUID playerId, @Param("status") PendingActionStatus status,
                            @Param("types") Collection<ActionType> types);
}
