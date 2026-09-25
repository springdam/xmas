package com.xmas.game.engine.repository;

import com.xmas.game.engine.domain.GameStatus;
import com.xmas.game.engine.entity.GameEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<GameEntity, UUID> {

    List<GameEntity> findByStatus(GameStatus status);
}
