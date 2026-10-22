package com.xmas.game.engine.repository;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.entity.PlayerSpacePartEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerSpacePartRepository extends JpaRepository<PlayerSpacePartEntity, UUID> {

    List<PlayerSpacePartEntity> findByPlayerId(UUID playerId);

    boolean existsByPlayerIdAndPart(UUID playerId, ActionType part);
}
