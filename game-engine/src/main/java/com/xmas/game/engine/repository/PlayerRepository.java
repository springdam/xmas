package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    List<PlayerEntity> findByGameId(UUID gameId);

    List<PlayerEntity> findByGameIdIn(Collection<UUID> gameIds);

    List<PlayerEntity> findByAccountId(UUID accountId);

    boolean existsByGameIdAndAccountId(UUID gameId, UUID accountId);

    Optional<PlayerEntity> findByGameIdAndAccountId(UUID gameId, UUID accountId);
}
