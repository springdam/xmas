package com.xmas.game.engine.diplomacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xmas.game.common.domain.Nation;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.domain.RelationStatus;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerRelationEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerRelationRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.newsfeed.service.NewsService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaDiplomacyResolverTest {

    private final UUID gameId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerRelationRepository relationRepository = mock(PlayerRelationRepository.class);

    private PlayerEntity actor;
    private PlayerEntity target;
    private JpaDiplomacyResolver resolver;

    @BeforeEach
    void setUp() {
        actor = PlayerEntity.builder().id(actorId).gameId(gameId).nation(Nation.RUSSIA)
                .popularity(50).cash(1_000_000).build();
        target = PlayerEntity.builder().id(targetId).gameId(gameId).nation(Nation.DENMARK)
                .popularity(50).cash(0).build();
        when(playerRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(playerRepository.findById(targetId)).thenReturn(Optional.of(target));

        GameRepository gameRepository = mock(GameRepository.class);
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());
        ResearchModifierService modifiers = mock(ResearchModifierService.class);
        when(modifiers.paradePraiseDecay(any(), anyDouble())).thenAnswer(inv -> inv.getArgument(1));

        resolver = new JpaDiplomacyResolver(playerRepository, relationRepository, gameRepository,
                new PopularityProperties(), modifiers, mock(NewsService.class));
    }

    @Test
    void smallGift_transfersCashButEarnsNoPopularity() {
        resolver.sendMoney(actorId, targetId, 1);

        assertThat(target.getCash()).isEqualTo(1);
        assertThat(actor.getPopularity()).isEqualTo(50);
        assertThat(actor.getPopularityBoostCount()).isZero();
    }

    @Test
    void largeGifts_earnDiminishingPopularity() {
        resolver.sendMoney(actorId, targetId, 50_000);
        resolver.sendMoney(actorId, targetId, 50_000);
        resolver.sendMoney(actorId, targetId, 50_000);

        // +3, then +3*0.6 = +2, then +3*0.36 = +1
        assertThat(actor.getPopularity()).isEqualTo(56);
        assertThat(actor.getPopularityBoostCount()).isEqualTo(3);
    }

    @Test
    void atWar_onlyWhenTheirRelationIsWar() {
        UUID low = actorId.compareTo(targetId) <= 0 ? actorId : targetId;
        UUID high = low == actorId ? targetId : actorId;
        assertThat(resolver.atWar(actorId, targetId)).isFalse();

        when(relationRepository.findByPlayerLowAndPlayerHigh(low, high)).thenReturn(Optional.of(
                PlayerRelationEntity.builder().playerLow(low).playerHigh(high).status(RelationStatus.WAR).build()));
        assertThat(resolver.atWar(actorId, targetId)).isTrue();
        assertThat(resolver.atWar(targetId, actorId)).isTrue();

        when(relationRepository.findByPlayerLowAndPlayerHigh(low, high)).thenReturn(Optional.of(
                PlayerRelationEntity.builder().playerLow(low).playerHigh(high).status(RelationStatus.PEACE).build()));
        assertThat(resolver.atWar(actorId, targetId)).isFalse();
    }
}
