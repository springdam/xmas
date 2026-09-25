package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerEconomyEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerEconomyRepository extends JpaRepository<PlayerEconomyEntity, UUID> {
}
