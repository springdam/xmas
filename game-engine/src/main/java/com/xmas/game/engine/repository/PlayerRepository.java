package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    List<PlayerEntity> findByGameId(UUID gameId);

    boolean existsByGameIdAndAccountId(UUID gameId, UUID accountId);

    Optional<PlayerEntity> findFirstByAccountIdOrderByJoinedAtDesc(UUID accountId);
}
