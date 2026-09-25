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
@Table(name = "player_military")
public class PlayerMilitaryEntity {

    @Id
    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "infantry", nullable = false)
    private int infantry;

    @Column(name = "tanks", nullable = false)
    private int tanks;

    @Column(name = "missile_batteries", nullable = false)
    private int missileBatteries;

    @Column(name = "air_defense_batteries", nullable = false)
    private int airDefenseBatteries;

    @Column(name = "stealth_bombers", nullable = false)
    private int stealthBombers;

    @Column(name = "special_ops", nullable = false)
    private int specialOps;
}
