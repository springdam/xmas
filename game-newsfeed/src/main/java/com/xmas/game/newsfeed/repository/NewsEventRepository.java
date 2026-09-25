package com.xmas.game.newsfeed.repository;

import com.xmas.game.newsfeed.entity.NewsEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsEventRepository extends JpaRepository<NewsEventEntity, UUID> {

    @Query("""
            select n from NewsEventEntity n
            where n.gameId = :gameId
              and (n.visibility = com.xmas.game.newsfeed.domain.NewsVisibility.PUBLIC
                   or (n.visibility = com.xmas.game.newsfeed.domain.NewsVisibility.INVOLVED_ONLY
                       and (n.actorPlayerId = :playerId or n.targetPlayerId = :playerId))
                   or (n.visibility = com.xmas.game.newsfeed.domain.NewsVisibility.ACTOR_ONLY
                       and n.actorPlayerId = :playerId))
            order by n.gameTime desc
            """)
    List<NewsEventEntity> feedForPlayer(@Param("playerId") UUID playerId, @Param("gameId") UUID gameId,
                                        Pageable pageable);
}
