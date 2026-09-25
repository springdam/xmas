package com.xmas.game.engine.repository;

import com.xmas.game.engine.entity.PlayerSpecialWeaponsEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerSpecialWeaponsRepository extends JpaRepository<PlayerSpecialWeaponsEntity, UUID> {
}
