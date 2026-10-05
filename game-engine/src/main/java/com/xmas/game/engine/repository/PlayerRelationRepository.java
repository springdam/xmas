package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerRelationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRelationRepository extends JpaRepository<PlayerRelationEntity, UUID> {

    Optional<PlayerRelationEntity> findByPlayerLowAndPlayerHigh(UUID playerLow, UUID playerHigh);

    List<PlayerRelationEntity> findByPlayerLowOrPlayerHigh(UUID playerLow, UUID playerHigh);

    /** At war with at least one nation that is still standing (a fallen enemy no longer counts). */
    @Query("""
            select count(r) > 0 from PlayerRelationEntity r, PlayerEntity other
            where r.status = com.xmas.game.engine.domain.RelationStatus.WAR
              and ((r.playerLow = :playerId and other.id = r.playerHigh)
                or (r.playerHigh = :playerId and other.id = r.playerLow))
              and other.eliminated = false
            """)
    boolean isAtWar(@Param("playerId") UUID playerId);
}
