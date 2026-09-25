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
@Table(name = "player_protection")
public class PlayerProtectionEntity {

    @Id
    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "doublegangers", nullable = false)
    private int doublegangers;

    @Column(name = "bunkers", nullable = false)
    private int bunkers;

    @Column(name = "bodyguards", nullable = false)
    private int bodyguards;

    @Column(name = "ninja_bodyguards", nullable = false)
    private int ninjaBodyguards;
}
