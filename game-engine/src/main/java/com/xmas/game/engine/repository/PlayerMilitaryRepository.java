package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerMilitaryRepository extends JpaRepository<PlayerMilitaryEntity, UUID> {
}
