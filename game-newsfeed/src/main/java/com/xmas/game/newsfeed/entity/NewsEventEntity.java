package com.xmas.game.newsfeed.entity;

import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
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
@Table(name = "news_event")
public class NewsEventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NewsType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private NewsVisibility visibility;

    @Column(name = "actor_player_id")
    private UUID actorPlayerId;

    @Column(name = "target_player_id")
    private UUID targetPlayerId;

    @Column(name = "headline", nullable = false, length = 2048)
    private String headline;

    @Column(name = "game_time", nullable = false)
    private Instant gameTime;
}
