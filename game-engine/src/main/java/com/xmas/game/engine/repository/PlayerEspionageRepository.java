package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerEspionageEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerEspionageRepository extends JpaRepository<PlayerEspionageEntity, UUID> {
}
