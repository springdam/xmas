package com.xmas.game.engine.entity;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.engine.domain.PendingActionStatus;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pending_action")
public class PendingActionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_player_id", nullable = false)
    private UUID actorPlayerId;

    @Column(name = "target_player_id")
    private UUID targetPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completes_at", nullable = false)
    private Instant completesAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PendingActionStatus status;
}
