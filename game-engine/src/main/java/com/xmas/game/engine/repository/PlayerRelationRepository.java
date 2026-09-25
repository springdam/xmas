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

    @Query("""
            select count(r) > 0 from PlayerRelationEntity r
            where r.status = com.xmas.game.engine.domain.RelationStatus.WAR
              and (r.playerLow = :playerId or r.playerHigh = :playerId)
            """)
    boolean isAtWar(@Param("playerId") UUID playerId);
}
