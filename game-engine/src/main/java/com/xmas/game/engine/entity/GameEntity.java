package com.xmas.game.engine.entity;

import com.xmas.game.engine.domain.GameStatus;
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
@Table(name = "game")
public class GameEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GameStatus status;

    @Column(name = "current_tick", nullable = false)
    private long currentTick;

    @Column(name = "game_time", nullable = false)
    private Instant gameTime;

    @Column(name = "tick_duration_millis", nullable = false)
    private long tickDurationMillis;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** The player who launched the Mars mission and won; null while the game is undecided. */
    @Column(name = "winner_player_id")
    private UUID winnerPlayerId;
}
