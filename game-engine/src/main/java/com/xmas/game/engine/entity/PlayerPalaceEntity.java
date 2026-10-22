package com.xmas.game.engine.entity;

import com.xmas.game.engine.domain.PalacePart;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A nation's throne room: a pure trophy, earned through milestones or bought, and damaged by rivals. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_palace")
public class PlayerPalaceEntity {

    @Id
    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "walls", nullable = false)
    private int walls;

    @Column(name = "floor", nullable = false)
    private int floor;

    @Column(name = "throne", nullable = false)
    private int throne;

    @Column(name = "pillars", nullable = false)
    private int pillars;

    @Column(name = "windows", nullable = false)
    private int windows;

    @Column(name = "treasures", nullable = false)
    private int treasures;

    /** Upgrades earned through milestones and not spent yet. */
    @Column(name = "tokens", nullable = false)
    private int tokens;

    /** Attacks won, counted toward the next victory token. */
    @Column(name = "attack_victories", nullable = false)
    private int attackVictories;

    /** Tick the nation's popularity last rose above the honour threshold; null while below it. */
    @Column(name = "popular_since_tick")
    private Long popularSinceTick;

    public int tier(PalacePart part) {
        return switch (part) {
            case WALLS -> walls;
            case FLOOR -> floor;
            case THRONE -> throne;
            case PILLARS -> pillars;
            case WINDOWS -> windows;
            case TREASURES -> treasures;
        };
    }

    public void setTier(PalacePart part, int tier) {
        switch (part) {
            case WALLS -> walls = tier;
            case FLOOR -> floor = tier;
            case THRONE -> throne = tier;
            case PILLARS -> pillars = tier;
            case WINDOWS -> windows = tier;
            case TREASURES -> treasures = tier;
        }
    }

    /** Sum of all tiers: 0..24. */
    public int score() {
        return walls + floor + throne + pillars + windows + treasures;
    }
}
