package com.xmas.game.engine.space;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.space.SpaceGateway;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerSpacePartEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerSpacePartRepository;
import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The Mars mission's built parts. Kept secret: only the builder ever hears about them. */
@Component
@RequiredArgsConstructor
public class JpaSpaceGateway implements SpaceGateway {

    private final PlayerSpacePartRepository repository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final NewsService newsService;

    @Override
    @Transactional(readOnly = true)
    public boolean hasPart(UUID playerId, ActionType part) {
        return repository.existsByPlayerIdAndPart(playerId, part);
    }

    @Override
    @Transactional
    public void addPart(UUID playerId, ActionType part) {
        repository.save(PlayerSpacePartEntity.builder().id(UUID.randomUUID()).playerId(playerId).part(part).build());
        playerRepository.findById(playerId).ifPresent(player -> newsService.record(player.getGameId(),
                NewsType.DIPLOMACY, NewsVisibility.ACTOR_ONLY, playerId, null,
                "Your engineers completed the %s for the Mars mission.".formatted(
                        part.name().replace("SPACE_", "").replace('_', ' ').toLowerCase()),
                gameTime(player)));
    }

    /** The parts a player has built, for their own eyes only. */
    @Transactional(readOnly = true)
    public List<ActionType> parts(UUID playerId) {
        return repository.findByPlayerId(playerId).stream().map(PlayerSpacePartEntity::getPart).toList();
    }

    private Instant gameTime(PlayerEntity player) {
        return gameRepository.findById(player.getGameId()).map(GameEntity::getGameTime).orElse(Instant.now());
    }
}
