package com.xmas.game.engine.entity;

import com.xmas.game.engine.domain.RelationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A diplomatic relation between two players, stored once per unordered pair with
 * playerLow &lt; playerHigh by UUID ordering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_relation")
public class PlayerRelationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "player_low", nullable = false)
    private UUID playerLow;

    @Column(name = "player_high", nullable = false)
    private UUID playerHigh;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RelationStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
