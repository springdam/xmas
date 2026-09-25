package com.xmas.game.engine.diplomacy;

import com.xmas.game.actions.diplomacy.DiplomacyResolver;
import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.domain.Popularity;
import com.xmas.game.engine.domain.RelationStatus;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerRelationEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerRelationRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JpaDiplomacyResolver implements DiplomacyResolver {

    private final PlayerRepository playerRepository;
    private final PlayerRelationRepository relationRepository;
    private final GameRepository gameRepository;
    private final PopularityProperties properties;
    private final ResearchModifierService researchModifierService;
    private final NewsService newsService;

    @Override
    @Transactional
    public void declareWar(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        setRelation(actorId, targetId, RelationStatus.WAR);
        actor.setPopularity(Popularity.clamp(actor.getPopularity() - properties.getDeclareWarPenalty()));
        playerRepository.save(actor);
        record(actor, target, NewsVisibility.PUBLIC,
                "%s declared war on %s.".formatted(actor.getNation(), target.getNation()));
    }

    @Override
    @Transactional
    public void declarePeace(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        setRelation(actorId, targetId, RelationStatus.PEACE);
        actor.setPopularity(Popularity.clamp(actor.getPopularity() + properties.getDeclarePeaceBonus()));
        playerRepository.save(actor);
        record(actor, target, NewsVisibility.PUBLIC,
                "%s made peace with %s.".formatted(actor.getNation(), target.getNation()));
    }

    @Override
    @Transactional
    public void sendMoney(UUID actorId, UUID targetId, long amount) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        target.setCash(target.getCash() + amount);
        actor.setPopularity(Popularity.clamp(actor.getPopularity() + properties.getSendMoneyBonus()));
        playerRepository.save(target);
        playerRepository.save(actor);
        record(actor, target, NewsVisibility.INVOLVED_ONLY,
                "%s sent $%d in aid to %s.".formatted(actor.getNation(), amount, target.getNation()));
    }

    @Override
    @Transactional
    public void holdParade(UUID actorId) {
        PlayerEntity actor = player(actorId);
        int gain = diminished(properties.getParadeBonus(), actor.getPopularityBoostCount(), decay(actorId));
        actor.setPopularity(Popularity.clamp(actor.getPopularity() + gain));
        actor.setPopularityBoostCount(actor.getPopularityBoostCount() + 1);
        playerRepository.save(actor);
        newsService.record(actor.getGameId(), NewsType.DIPLOMACY, NewsVisibility.PUBLIC, actorId, null,
                "%s held a grand military parade, rallying popular support (+%d).".formatted(actor.getNation(), gain),
                gameTime(actor));
    }

    @Override
    @Transactional
    public void praiseLeader(UUID actorId, UUID targetId) {
        PlayerEntity actor = player(actorId);
        PlayerEntity target = player(targetId);
        int gain = diminished(properties.getPraiseBonus(), target.getPopularityBoostCount(), decay(actorId));
        target.setPopularity(Popularity.clamp(target.getPopularity() + gain));
        target.setPopularityBoostCount(target.getPopularityBoostCount() + 1);
        playerRepository.save(target);
        record(actor, target, NewsVisibility.PUBLIC,
                "%s publicly praised %s's leadership (+%d).".formatted(actor.getNation(), target.getNation(), gain));
    }

    @Override
    @Transactional
    public void holdElection(UUID actorId) {
        PlayerEntity actor = player(actorId);
        long currentTick = gameRepository.findById(actor.getGameId())
                .map(GameEntity::getCurrentTick)
                .orElse(0L);
        actor.setPopularityBoostCount(0);
        actor.setFrozenUntilTick(currentTick + properties.getElectionFreezeTicks());
        playerRepository.save(actor);
        newsService.record(actor.getGameId(), NewsType.DIPLOMACY, NewsVisibility.PUBLIC, actorId, null,
                "%s held a national election, renewing the leader's mandate.".formatted(actor.getNation()),
                gameTime(actor));
    }

    private int diminished(int baseBonus, int boostCount, double decay) {
        return (int) Math.round(baseBonus * Math.pow(decay, boostCount));
    }

    private double decay(UUID actorId) {
        return researchModifierService.paradePraiseDecay(actorId, properties.getParadePraiseDecay());
    }

    private void setRelation(UUID a, UUID b, RelationStatus status) {
        UUID low = a.compareTo(b) <= 0 ? a : b;
        UUID high = a.compareTo(b) <= 0 ? b : a;
        PlayerRelationEntity relation = relationRepository.findByPlayerLowAndPlayerHigh(low, high)
                .orElseGet(() -> PlayerRelationEntity.builder()
                        .id(UUID.randomUUID())
                        .playerLow(low)
                        .playerHigh(high)
                        .build());
        relation.setStatus(status);
        relation.setUpdatedAt(Instant.now());
        relationRepository.save(relation);
    }

    private void record(PlayerEntity actor, PlayerEntity target, NewsVisibility visibility, String headline) {
        newsService.record(actor.getGameId(), NewsType.DIPLOMACY, visibility,
                actor.getId(), target.getId(), headline, gameTime(actor));
        log.info("Diplomacy {} -> {}: {}", actor.getId(), target.getId(), headline);
    }

    private Instant gameTime(PlayerEntity player) {
        return gameRepository.findById(player.getGameId())
                .map(GameEntity::getGameTime)
                .orElse(Instant.now());
    }

    private PlayerEntity player(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
    }
}
