package com.xmas.game.engine.repository;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.entity.PlayerResearchEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerResearchRepository extends JpaRepository<PlayerResearchEntity, UUID> {

    List<PlayerResearchEntity> findByPlayerId(UUID playerId);

    Optional<PlayerResearchEntity> findByPlayerIdAndResearchType(UUID playerId, ActionType researchType);
}
