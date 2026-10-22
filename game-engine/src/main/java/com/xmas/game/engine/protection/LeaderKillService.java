package com.xmas.game.engine.protection;

import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerProtectionEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerProtectionRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xmas.game.engine.palace.PalaceService;

/**
 * Applies a successful leader-kill attempt. A doppelganger absorbs the hit (one
 * is consumed and the leader survives); otherwise the nation is eliminated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderKillService {

    private final PlayerRepository playerRepository;
    private final PlayerProtectionRepository protectionRepository;
    private final GameRepository gameRepository;
    private final NewsService newsService;
    private final PalaceService palaceService;

    @Transactional
    public boolean attemptKill(PlayerEntity actor, PlayerEntity target, NewsType newsType, String method) {
        PlayerProtectionEntity protection = protection(target.getId());
        if (protection.getDoublegangers() > 0) {
            protection.setDoublegangers(protection.getDoublegangers() - 1);
            protectionRepository.save(protection);
            palaceService.award(target.getId(), "surviving an attempt on your life");
            record(actor, target, newsType,
                    "A body double died in %s's place, foiling an attempted %s by %s.".formatted(
                            target.getNation(), method, actor.getNation()));
            return false;
        }
        target.setEliminated(true);
        playerRepository.save(target);
        record(actor, target, newsType,
                "%s's leader was killed by %s carried out by %s. The nation has fallen.".formatted(
                        target.getNation(), method, actor.getNation()));
        log.info("Player {} ({}) eliminated by {} via {}", target.getId(), target.getNation(), actor.getId(), method);
        return true;
    }

    private void record(PlayerEntity actor, PlayerEntity target, NewsType newsType, String headline) {
        newsService.record(target.getGameId(), newsType, NewsVisibility.PUBLIC,
                actor.getId(), target.getId(), headline, gameTime(target));
    }

    private Instant gameTime(PlayerEntity player) {
        return gameRepository.findById(player.getGameId())
                .map(GameEntity::getGameTime)
                .orElse(Instant.now());
    }

    private PlayerProtectionEntity protection(UUID playerId) {
        return protectionRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player protection not found: " + playerId));
    }
}
