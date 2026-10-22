package com.xmas.game.engine.palace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.common.domain.Nation;
import com.xmas.game.common.error.BadRequestException;
import com.xmas.game.engine.config.PalaceProperties;
import com.xmas.game.engine.domain.PalacePart;
import com.xmas.game.engine.domain.PalaceStyle;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerPalaceEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerPalaceRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PalaceServiceTest {

    private final UUID gameId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private final PlayerPalaceRepository palaceRepository = mock(PlayerPalaceRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final NewsService newsService = mock(NewsService.class);

    private PlayerEntity player;
    private PlayerPalaceEntity palace;
    private double roll = 0.0;
    private PalaceService service;

    @BeforeEach
    void setUp() {
        player = PlayerEntity.builder().id(playerId).gameId(gameId).nation(Nation.FRANCE)
                .cash(100_000).popularity(50).build();
        palace = PlayerPalaceEntity.builder().playerId(playerId).build();
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(palaceRepository.findById(playerId)).thenReturn(Optional.of(palace));
        GameRepository gameRepository = mock(GameRepository.class);
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());
        service = new PalaceService(palaceRepository, playerRepository, gameRepository,
                new PalaceProperties(), newsService, () -> roll);
    }

    @Test
    void anEarnedTokenPaysForAnUpgrade() {
        palace.setTokens(1);

        service.upgrade(playerId, PalacePart.THRONE, false);

        assertThat(palace.getThrone()).isEqualTo(1);
        assertThat(palace.getTokens()).isZero();
        assertThat(player.getCash()).isEqualTo(100_000);
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.PUBLIC), eq(playerId), isNull(),
                eq("FRANCE's palace now boasts a wooden throne."), any());
    }

    @Test
    void cashBuysTheNextTierAtItsPrice() {
        palace.setPillars(1);

        service.upgrade(playerId, PalacePart.PILLARS, true);

        assertThat(palace.getPillars()).isEqualTo(2);
        assertThat(player.getCash()).isEqualTo(50_000);   // tier 2 costs $50k
        verify(newsService).record(any(), any(), any(), any(), any(),
                eq("FRANCE's palace now boasts stone pillars."), any());
    }

    @Test
    void upgradesAreRefusedWithoutTokenCashOrRoom() {
        assertThatThrownBy(() -> service.upgrade(playerId, PalacePart.FLOOR, false))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("No palace improvement earned");

        palace.setFloor(2);   // tier 3 costs $120k, more than the $100k in the treasury
        assertThatThrownBy(() -> service.upgrade(playerId, PalacePart.FLOOR, true))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Insufficient funds");

        palace.setFloor(4);
        palace.setTokens(1);
        assertThatThrownBy(() -> service.upgrade(playerId, PalacePart.FLOOR, false))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("as grand as it gets");
    }

    @Test
    void damageOnlyHitsPartsThatStillHaveTiers() {
        palace.setThrone(2);

        service.damage(playerId, 3, "Vandals defaced");

        assertThat(palace.getThrone()).isZero();
        assertThat(palace.score()).isZero();
        verify(newsService).record(any(), any(), eq(NewsVisibility.PUBLIC), isNull(), eq(playerId),
                eq("Vandals defaced FRANCE's palace, damaging the throne."), any());
    }

    @Test
    void aBarePalaceCannotBeDamagedAndMakesNoNews() {
        service.damage(playerId, 2, "Vandals defaced");

        verify(newsService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void everyThirdVictoryEarnsAToken() {
        service.recordAttackVictory(playerId);
        service.recordAttackVictory(playerId);
        assertThat(palace.getTokens()).isZero();

        service.recordAttackVictory(playerId);
        assertThat(palace.getTokens()).isEqualTo(1);
        verify(newsService).record(any(), any(), eq(NewsVisibility.ACTOR_ONLY), eq(playerId), isNull(),
                argThat(h -> h.contains("3 victories in battle")), any());
    }

    @Test
    void tenMinutesOfHighPopularityEarnAToken() {
        player.setPopularity(80);
        GameEntity game = GameEntity.builder().id(gameId).currentTick(10).gameTime(Instant.now()).build();

        service.onTick(game, player);                     // streak starts at tick 10
        game.setCurrentTick(129);
        service.onTick(game, player);
        assertThat(palace.getTokens()).isZero();
        game.setCurrentTick(130);                         // 120 ticks later
        service.onTick(game, player);
        assertThat(palace.getTokens()).isEqualTo(1);

        player.setPopularity(60);                         // dropping below resets the streak
        service.onTick(game, player);
        assertThat(palace.getPopularSinceTick()).isNull();
    }

    @Test
    void stayingInPowerEarnsATokenEveryHalfHour() {
        GameEntity game = GameEntity.builder().id(gameId).currentTick(360).gameTime(Instant.now()).build();

        service.onTick(game, player);

        assertThat(palace.getTokens()).isEqualTo(1);
    }

    @Test
    void aBadDefeatIsLikelierToCostATier() {
        palace.setWalls(3);
        roll = 0.5;

        service.onBattleLost(playerId, 0.3, Nation.CHINA);   // 0.5 >= 0.3: walls survive
        assertThat(palace.getWalls()).isEqualTo(3);
        service.onBattleLost(playerId, 0.7, Nation.CHINA);   // 0.5 < 0.7: looted
        assertThat(palace.getWalls()).isEqualTo(2);
    }

    @Test
    void aStyleIsChosenOnceAndNamesTheTiers() {
        service.chooseStyle(playerId, PalaceStyle.ASIAN);
        assertThat(palace.getStyle()).isEqualTo(PalaceStyle.ASIAN);
        assertThatThrownBy(() -> service.chooseStyle(playerId, PalaceStyle.CASTLE))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("already chosen");

        palace.setThrone(1);
        service.upgrade(playerId, PalacePart.THRONE, true);
        verify(newsService).record(any(), any(), any(), any(), any(),
                eq("FRANCE's palace now boasts a lacquered throne."), any());
    }

    @Test
    void valueIsWhatEveryBuiltTierCost() {
        assertThat(service.value(palace)).isZero();

        palace.setThrone(2);   // 20k + 50k
        palace.setWalls(4);    // 20k + 50k + 120k + 250k
        assertThat(service.value(palace)).isEqualTo(70_000 + 440_000);
    }
}
