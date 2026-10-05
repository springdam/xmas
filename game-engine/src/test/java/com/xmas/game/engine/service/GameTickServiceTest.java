package com.xmas.game.engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.domain.ActionContext;
import com.xmas.game.actions.domain.ActionDefinition;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.service.ActionDefinitionCatalog;
import com.xmas.game.actions.service.ActionHandlerRegistry;
import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.config.UpkeepProperties;
import com.xmas.game.engine.economy.IncomeService;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.domain.GameStatus;
import com.xmas.game.engine.domain.PendingActionStatus;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PendingActionEntity;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.military.UpkeepService;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PendingActionRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.engine.repository.PlayerRelationRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameTickServiceTest {

    private GameRepository gameRepository;
    private PlayerRepository playerRepository;
    private PlayerEconomyRepository economyRepository;
    private PlayerRelationRepository relationRepository;
    private PendingActionRepository pendingActionRepository;
    private ActionHandlerRegistry handlerRegistry;
    private ActionDefinitionCatalog definitionCatalog;
    private ResearchModifierService researchModifierService;
    private NewsService newsService;
    private PlayerMilitaryRepository militaryRepository;
    private GameTickService tickService;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        playerRepository = mock(PlayerRepository.class);
        economyRepository = mock(PlayerEconomyRepository.class);
        relationRepository = mock(PlayerRelationRepository.class);
        pendingActionRepository = mock(PendingActionRepository.class);
        handlerRegistry = mock(ActionHandlerRegistry.class);
        definitionCatalog = mock(ActionDefinitionCatalog.class);
        researchModifierService = mock(ResearchModifierService.class);
        newsService = mock(NewsService.class);
        militaryRepository = mock(PlayerMilitaryRepository.class);
        when(researchModifierService.economyMultiplier(any())).thenReturn(1.0);
        when(researchModifierService.coupChanceMultiplier(any())).thenReturn(1.0);
        when(researchModifierService.factoryIncomeMultiplier(any())).thenReturn(1.0);
        when(researchModifierService.bankIncomeMultiplier(any())).thenReturn(1.0);
        when(researchModifierService.warEconomyMultiplier(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(1.0);
        RandomSource random = () -> 0.0; // coup roll always fires when a player is vulnerable
        IncomeService incomeService = new IncomeService(new EconomyProperties(), researchModifierService,
                new PopularityProperties(), relationRepository);
        tickService = new GameTickService(gameRepository, playerRepository, economyRepository,
                pendingActionRepository, incomeService, handlerRegistry,
                definitionCatalog, researchModifierService, new PopularityProperties(), random, newsService,
                militaryRepository, new UpkeepService(new UpkeepProperties()));
    }

    @Test
    void tick_advancesClockAndPaysIncome() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameEntity game = GameEntity.builder()
                .id(gameId).status(GameStatus.RUNNING)
                .currentTick(4).gameTime(Instant.parse("2026-01-01T00:00:00Z"))
                .tickDurationMillis(5000).createdAt(Instant.now())
                .build();
        PlayerEntity player = PlayerEntity.builder()
                .id(playerId).gameId(gameId).accountId(UUID.randomUUID())
                .cash(100).popularity(50).admin(false).joinedAt(Instant.now())
                .build();
        PlayerEconomyEntity economy = PlayerEconomyEntity.builder()
                .playerId(playerId).farms(2).factories(1).robotFactories(0).banks(0)
                .build();

        when(gameRepository.findByStatus(GameStatus.RUNNING)).thenReturn(List.of(game));
        when(playerRepository.findByGameId(gameId)).thenReturn(List.of(player));
        when(economyRepository.findById(playerId)).thenReturn(Optional.of(economy));
        when(pendingActionRepository
                .findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(eq(PendingActionStatus.PENDING), any()))
                .thenReturn(List.of());

        tickService.tick();

        // base 1000 + farms 2*500 + factories 1*1500 = 3500
        assertThat(player.getCash()).isEqualTo(3600);
        assertThat(game.getCurrentTick()).isEqualTo(5);
        assertThat(game.getGameTime()).isEqualTo(Instant.parse("2026-01-01T00:00:05Z"));
    }

    @Test
    void tick_overthrowsPlayerBelowCoupThreshold() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameEntity game = GameEntity.builder()
                .id(gameId).status(GameStatus.RUNNING).currentTick(1)
                .gameTime(Instant.parse("2026-01-01T00:00:00Z")).tickDurationMillis(5000).createdAt(Instant.now())
                .build();
        PlayerEntity player = PlayerEntity.builder()
                .id(playerId).gameId(gameId).accountId(UUID.randomUUID())
                .cash(100).popularity(10).admin(false).eliminated(false).joinedAt(Instant.now())
                .build();

        when(gameRepository.findByStatus(GameStatus.RUNNING)).thenReturn(List.of(game));
        when(playerRepository.findByGameId(gameId)).thenReturn(List.of(player));
        when(pendingActionRepository
                .findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(eq(PendingActionStatus.PENDING), any()))
                .thenReturn(List.of());

        tickService.tick();

        assertThat(player.isEliminated()).isTrue();
        assertThat(player.getCash()).isEqualTo(100); // no income paid to an overthrown player
        verify(newsService).record(eq(gameId), any(), any(), eq(playerId), isNull(), any(), any());
    }

    @Test
    void tick_industryBeyondWhatFarmsFeedEarnsHalf() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameEntity game = GameEntity.builder()
                .id(gameId).status(GameStatus.RUNNING).currentTick(1)
                .gameTime(Instant.parse("2026-01-01T00:00:00Z")).tickDurationMillis(5000).createdAt(Instant.now())
                .build();
        PlayerEntity player = PlayerEntity.builder()
                .id(playerId).gameId(gameId).accountId(UUID.randomUUID())
                .cash(0).popularity(50).joinedAt(Instant.now())
                .build();
        // 1 farm feeds 3: both robot factories first, then 1 of the 2 factories.
        PlayerEconomyEntity economy = PlayerEconomyEntity.builder()
                .playerId(playerId).farms(1).factories(2).robotFactories(2).banks(0)
                .build();

        when(gameRepository.findByStatus(GameStatus.RUNNING)).thenReturn(List.of(game));
        when(playerRepository.findByGameId(gameId)).thenReturn(List.of(player));
        when(economyRepository.findById(playerId)).thenReturn(Optional.of(economy));
        when(pendingActionRepository
                .findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(eq(PendingActionStatus.PENDING), any()))
                .thenReturn(List.of());

        tickService.tick();

        // base 1000 + farm 500 + robots 2*4000 + fed factory 1500 + unfed factory 1500*0.5 = 11750
        assertThat(player.getCash()).isEqualTo(11_750);
    }

    /** One running game with one player who has the given cash, 2 farms and the given army. */
    private PlayerEntity armedPlayer(long cash, PlayerMilitaryEntity.PlayerMilitaryEntityBuilder army) {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameEntity game = GameEntity.builder()
                .id(gameId).status(GameStatus.RUNNING).currentTick(1)
                .gameTime(Instant.parse("2026-01-01T00:00:00Z")).tickDurationMillis(5000).createdAt(Instant.now())
                .build();
        PlayerEntity player = PlayerEntity.builder()
                .id(playerId).gameId(gameId).accountId(UUID.randomUUID())
                .cash(cash).popularity(50).joinedAt(Instant.now())
                .build();
        when(gameRepository.findByStatus(GameStatus.RUNNING)).thenReturn(List.of(game));
        when(playerRepository.findByGameId(gameId)).thenReturn(List.of(player));
        when(economyRepository.findById(playerId)).thenReturn(Optional.of(
                PlayerEconomyEntity.builder().playerId(playerId).farms(2).build()));
        when(militaryRepository.findById(playerId)).thenReturn(Optional.of(army.playerId(playerId).build()));
        when(pendingActionRepository
                .findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(eq(PendingActionStatus.PENDING), any()))
                .thenReturn(List.of());
        return player;
    }

    @Test
    void tick_paysArmyUpkeepFromIncome() {
        PlayerEntity player = armedPlayer(10_000,
                PlayerMilitaryEntity.builder().infantry(10).tanks(5).stealthBombers(2).specialOps(1));

        tickService.tick();

        // income 1000 + 2*500 = 2000; upkeep 10*20 + 5*80 + 2*250 + 1*200 = 1300
        assertThat(player.getCash()).isEqualTo(10_700);
    }

    @Test
    void tick_unpaidArmyDeserts() {
        PlayerMilitaryEntity.PlayerMilitaryEntityBuilder army =
                PlayerMilitaryEntity.builder().infantry(500).tanks(40).stealthBombers(3);
        PlayerEntity player = armedPlayer(0, army);
        PlayerMilitaryEntity military = militaryRepository.findById(player.getId()).orElseThrow();

        tickService.tick();

        // income 2000 < upkeep 10000 + 3200 + 750: broke, 10% of each type (rounded up) walks off
        assertThat(player.getCash()).isZero();
        assertThat(military.getInfantry()).isEqualTo(450);
        assertThat(military.getTanks()).isEqualTo(36);
        assertThat(military.getStealthBombers()).isEqualTo(2);
    }

    @Test
    void tick_warStrainRecoversOnlyOnRecoveryTicks() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameEntity game = GameEntity.builder()
                .id(gameId).status(GameStatus.RUNNING).currentTick(10)
                .gameTime(Instant.parse("2026-01-01T00:00:00Z")).tickDurationMillis(5000).createdAt(Instant.now())
                .build();
        PlayerEntity player = PlayerEntity.builder()
                .id(playerId).gameId(gameId).accountId(UUID.randomUUID())
                .cash(0).popularity(50).warStrain(2).joinedAt(Instant.now())
                .build();

        when(gameRepository.findByStatus(GameStatus.RUNNING)).thenReturn(List.of(game));
        when(playerRepository.findByGameId(gameId)).thenReturn(List.of(player));
        when(pendingActionRepository
                .findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(eq(PendingActionStatus.PENDING), any()))
                .thenReturn(List.of());

        tickService.tick(); // tick 11
        assertThat(player.getWarStrain()).isEqualTo(2);
        tickService.tick(); // tick 12: recovery happens every 12 ticks
        assertThat(player.getWarStrain()).isEqualTo(1);
    }

    @Test
    void tick_resolvesDueActionAndMarksCompleted() {
        UUID gameId = UUID.randomUUID();
        GameEntity game = GameEntity.builder()
                .id(gameId).status(GameStatus.RUNNING).currentTick(0)
                .gameTime(Instant.now()).tickDurationMillis(5000).createdAt(Instant.now())
                .build();
        UUID actor = UUID.randomUUID();
        PendingActionEntity pending = PendingActionEntity.builder()
                .id(UUID.randomUUID()).actorPlayerId(actor).actionType(ActionType.BUILD_FACTORY)
                .quantity(1).startedAt(Instant.now()).completesAt(Instant.now().minusSeconds(1))
                .status(PendingActionStatus.PENDING)
                .build();
        ActionHandler handler = mock(ActionHandler.class);

        when(gameRepository.findByStatus(GameStatus.RUNNING)).thenReturn(List.of(game));
        when(playerRepository.findByGameId(gameId)).thenReturn(List.of());
        when(pendingActionRepository
                .findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(eq(PendingActionStatus.PENDING), any()))
                .thenReturn(List.of(pending));
        when(handlerRegistry.get(ActionType.BUILD_FACTORY)).thenReturn(handler);
        when(definitionCatalog.getDefinition(ActionType.BUILD_FACTORY))
                .thenReturn(ActionDefinition.builder().actionType(ActionType.BUILD_FACTORY).build());

        tickService.tick();

        verify(handler).onComplete(any(ActionContext.class));
        assertThat(pending.getStatus()).isEqualTo(PendingActionStatus.COMPLETED);
    }
}
