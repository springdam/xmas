package com.xmas.game.engine.entity;

import com.xmas.game.common.domain.Nation;
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
@Table(name = "player")
public class PlayerEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nation")
    private Nation nation;

    @Column(name = "cash", nullable = false)
    private long cash;

    @Column(name = "popularity", nullable = false)
    private int popularity;

    @Column(name = "admin", nullable = false)
    private boolean admin;

    @Column(name = "eliminated", nullable = false)
    private boolean eliminated;

    @Column(name = "popularity_boost_count", nullable = false)
    private int popularityBoostCount;

    @Column(name = "frozen_until_tick", nullable = false)
    private long frozenUntilTick;

    /** Tick of the player's last parade, or null if they never held one (parades have a cooldown). */
    @Column(name = "last_parade_tick")
    private Long lastParadeTick;

    /** Recent battles lost; each makes the next defeat hurt popularity less. Recovers over time. */
    @Column(name = "war_strain", nullable = false)
    private int warStrain;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
}
