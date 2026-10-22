package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerPalaceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerPalaceRepository extends JpaRepository<PlayerPalaceEntity, UUID> {
}
