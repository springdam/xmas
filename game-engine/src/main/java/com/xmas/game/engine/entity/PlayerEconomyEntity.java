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
@Table(name = "player_economy")
public class PlayerEconomyEntity {

    @Id
    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "farms", nullable = false)
    private int farms;

    @Column(name = "factories", nullable = false)
    private int factories;

    @Column(name = "robot_factories", nullable = false)
    private int robotFactories;

    @Column(name = "banks", nullable = false)
    private int banks;
}
