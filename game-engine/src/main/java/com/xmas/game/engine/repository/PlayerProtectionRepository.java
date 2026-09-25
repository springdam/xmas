package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerProtectionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProtectionRepository extends JpaRepository<PlayerProtectionEntity, UUID> {
}
