package com.xmas.game.newsfeed.mapper;

import com.xmas.game.newsfeed.entity.NewsEventEntity;
import java.time.ZoneOffset;

public final class NewsMapper {

    private NewsMapper() {
    }

    public static com.xmas.game.newsfeed.api.model.NewsEvent toDto(NewsEventEntity e) {
        return new com.xmas.game.newsfeed.api.model.NewsEvent()
                .id(e.getId())
                .gameId(e.getGameId())
                .type(com.xmas.game.newsfeed.api.model.NewsType.fromValue(e.getType().name()))
                .visibility(com.xmas.game.newsfeed.api.model.NewsVisibility.fromValue(e.getVisibility().name()))
                .actorPlayerId(e.getActorPlayerId())
                .targetPlayerId(e.getTargetPlayerId())
                .headline(e.getHeadline())
                .gameTime(e.getGameTime().atOffset(ZoneOffset.UTC));
    }
}
