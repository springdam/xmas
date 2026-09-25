package com.xmas.game.newsfeed.service;

import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.entity.NewsEventEntity;
import com.xmas.game.newsfeed.repository.NewsEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsService {

    private static final int MAX_FEED_SIZE = 100;

    private final NewsEventRepository repository;

    @Transactional
    public NewsEventEntity record(UUID gameId, NewsType type, NewsVisibility visibility,
                                  UUID actorPlayerId, UUID targetPlayerId, String headline, Instant gameTime) {
        return repository.save(NewsEventEntity.builder()
                .id(UUID.randomUUID())
                .gameId(gameId)
                .type(type)
                .visibility(visibility)
                .actorPlayerId(actorPlayerId)
                .targetPlayerId(targetPlayerId)
                .headline(headline)
                .gameTime(gameTime)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NewsEventEntity> feedForPlayer(UUID playerId, UUID gameId, int limit) {
        int capped = Math.min(Math.max(limit, 1), MAX_FEED_SIZE);
        return repository.feedForPlayer(playerId, gameId, PageRequest.of(0, capped));
    }
}
