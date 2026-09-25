package com.xmas.game.engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_special_weapons")
public class PlayerSpecialWeaponsEntity {

    @Id
    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "nukes", nullable = false)
    private int nukes;

    @Column(name = "espionage_satellites", nullable = false)
    private int espionageSatellites;
}
