package com.xmas.game.newsfeed.rest;

import com.xmas.game.common.error.ForbiddenException;
import com.xmas.game.common.game.PlayerDirectory;
import com.xmas.game.common.security.SessionIdentity;
import com.xmas.game.newsfeed.api.NewsfeedApi;
import com.xmas.game.newsfeed.api.model.NewsEvent;
import com.xmas.game.newsfeed.mapper.NewsMapper;
import com.xmas.game.newsfeed.service.NewsService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NewsController implements NewsfeedApi {

    private final NewsService newsService;
    private final SessionIdentity sessionIdentity;
    private final PlayerDirectory playerDirectory;

    @Override
    public ResponseEntity<List<NewsEvent>> getPlayerNews(UUID playerId, String xSessionToken, Integer limit) {
        if (!sessionIdentity.requirePlayer(xSessionToken).equals(playerId)) {
            throw new ForbiddenException("You may only read your own news feed");
        }
        UUID gameId = playerDirectory.requireGameId(playerId);
        List<NewsEvent> body = newsService.feedForPlayer(playerId, gameId, limit == null ? 50 : limit)
                .stream()
                .map(NewsMapper::toDto)
                .toList();
        return ResponseEntity.ok(body);
    }
}
