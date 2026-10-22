package com.xmas.game.engine.espionage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.common.domain.Nation;
import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.config.EspionageProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.config.SpecialForcesProperties;
import com.xmas.game.engine.economy.BuildingDamageService;
import com.xmas.game.engine.economy.IncomeService;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerEspionageEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.entity.PlayerResearchEntity;
import com.xmas.game.engine.protection.LeaderKillService;
import com.xmas.game.engine.protection.ProtectionService;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerEspionageRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaEspionageResolverTest {

    private final UUID gameId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerEspionageRepository espionageRepository = mock(PlayerEspionageRepository.class);
    private final PlayerEconomyRepository economyRepository = mock(PlayerEconomyRepository.class);
    private final PlayerMilitaryRepository militaryRepository = mock(PlayerMilitaryRepository.class);
    private final NewsService newsService = mock(NewsService.class);
    private final IncomeService incomeService = mock(IncomeService.class);
    private final PlayerResearchRepository researchRepository = mock(PlayerResearchRepository.class);
    private final ResearchGateway researchGateway = mock(ResearchGateway.class);
    private long targetCash = 0;
    private PlayerEntity actor;
    private PlayerEntity target;

    private final PlayerEspionageEntity actorEsp = PlayerEspionageEntity.builder().playerId(actorId).spies(10).counterIntel(0).build();
    private final PlayerEspionageEntity targetEsp = PlayerEspionageEntity.builder().playerId(targetId).spies(0).counterIntel(10).build();
    private final PlayerEconomyEntity targetEco = PlayerEconomyEntity.builder()
            .playerId(targetId).farms(5).factories(10).robotFactories(5).banks(5).build();

    /** A resolver whose dice come up in the given order. */
    private JpaEspionageResolver resolver(double... rolls) {
        int[] next = {0};
        RandomSource random = () -> rolls[next[0]++];

        actor = PlayerEntity.builder().id(actorId).gameId(gameId).nation(Nation.RUSSIA).popularity(50).build();
        target = PlayerEntity.builder().id(targetId).gameId(gameId).nation(Nation.DENMARK).popularity(50)
                .cash(targetCash).build();
        when(playerRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(playerRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(espionageRepository.findById(actorId)).thenReturn(Optional.of(actorEsp));
        when(espionageRepository.findById(targetId)).thenReturn(Optional.of(targetEsp));
        when(economyRepository.findById(targetId)).thenReturn(Optional.of(targetEco));
        when(militaryRepository.findById(targetId)).thenReturn(Optional.of(PlayerMilitaryEntity.builder()
                .playerId(targetId).infantry(40).tanks(10).missileBatteries(0).airDefenseBatteries(0)
                .stealthBombers(0).specialOps(0).build()));

        GameRepository gameRepository = mock(GameRepository.class);
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());
        ResearchModifierService modifiers = mock(ResearchModifierService.class);
        when(modifiers.espionageMultiplier(any())).thenReturn(1.0);
        when(modifiers.counterIntelMultiplier(any())).thenReturn(1.0);
        when(modifiers.popularityDamageMultiplier(any())).thenReturn(1.0);

        return new JpaEspionageResolver(playerRepository, espionageRepository, economyRepository,
                militaryRepository, researchRepository, gameRepository,
                random, new EspionageProperties(), new PopularityProperties(), modifiers,
                researchGateway, mock(ProtectionService.class), mock(LeaderKillService.class),
                new SpecialForcesProperties(), newsService,
                // building rolls of 0.15 against cyber strength 0.2 x fragility:
                // farms 0.05, factories 0.12 survive; banks 0.16, robot factories 0.2 fall
                new BuildingDamageService(new EconomyProperties(), () -> 0.15),
                incomeService);
    }

    @Test
    void failedOperationLosesCaughtSpiesAndNamesTheAttacker() {
        // 10 spies vs 10 counter-intel: success 10/22 = 45%, roll 0.9 fails.
        // Capture chance 0.5 * 10/20 = 0.25 per spy: three of ten rolls are below it.
        resolver(0.9, 0.1, 0.1, 0.1, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9, 0.9).performCyberAttack(actorId, targetId);

        assertThat(actorEsp.getSpies()).isEqualTo(7);
        assertThat(targetEco.getFactories()).isEqualTo(10);
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.INVOLVED_ONLY), eq(actorId), eq(targetId),
                argThat(h -> h.contains("caught 3 spies from RUSSIA attempting a cyber attack")), any());
    }

    @Test
    void failureWithNobodyCaughtKeepsTheAttackerAnonymous() {
        targetEsp.setCounterIntel(0);   // nobody to catch them: capture chance 0
        // success 10/12, roll 0.95 fails; ten capture rolls all miss
        resolver(0.95, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).performSabotage(actorId, targetId);

        assertThat(actorEsp.getSpies()).isEqualTo(10);
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.INVOLVED_ONLY), isNull(), eq(targetId),
                argThat(h -> h.startsWith("Foreign agents attempted sabotage against DENMARK")), any());
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.ACTOR_ONLY), eq(actorId), eq(targetId),
                argThat(h -> h.contains("None of your spies were caught")), any());
        verify(espionageRepository, never()).save(any());
    }

    @Test
    void successCostsNoSpies() {
        resolver(0.0).performCyberAttack(actorId, targetId);

        assertThat(actorEsp.getSpies()).isEqualTo(10);
        assertThat(targetEco.getFarms()).isEqualTo(5);
        assertThat(targetEco.getFactories()).isEqualTo(10);
        assertThat(targetEco.getBanks()).isZero();
        assertThat(targetEco.getRobotFactories()).isZero();
        verify(espionageRepository, never()).save(any());
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.INVOLVED_ONLY), eq(actorId), eq(targetId),
                argThat(h -> h.endsWith("destroying 5 banks and 5 robot factories.")), any());
    }

    @Test
    void successfulReconIsSecretAndCostsNoSpies() {
        resolver(0.0, 0.5, 0.5, 0.5, 0.5, 0.5).gatherIntelligence(actorId, targetId);

        assertThat(actorEsp.getSpies()).isEqualTo(10);
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.ACTOR_ONLY), eq(actorId), eq(targetId),
                argThat(h -> h.startsWith("INTEL on DENMARK")), any());
        verify(newsService, never()).record(any(), any(), eq(NewsVisibility.INVOLVED_ONLY), any(), any(), any(), any());
    }

    @Test
    void heistTakesAFifthOfTheTreasury() {
        targetCash = 100_000;
        when(incomeService.perTick(eq(targetId), any(), eq(50))).thenReturn(10_000L);
        resolver(0.0).performHeist(actorId, targetId);

        assertThat(actor.getCash()).isEqualTo(20_000);
        assertThat(target.getCash()).isEqualTo(80_000);
    }

    @Test
    void heistOnAHoardIsCappedAtTwoMinutesOfIncome() {
        targetCash = 1_000_000;
        when(incomeService.perTick(eq(targetId), any(), eq(50))).thenReturn(5_000L);
        resolver(0.0).performHeist(actorId, targetId);

        // a fifth would be 200k; 24 ticks of 5k income caps it at 120k
        assertThat(actor.getCash()).isEqualTo(120_000);
        assertThat(target.getCash()).isEqualTo(880_000);
    }

    private PlayerResearchEntity tech(ActionType type, int level) {
        return PlayerResearchEntity.builder().playerId(targetId).researchType(type).level(level).build();
    }

    @Test
    void stealingTakesOneLevelOfATechTheTargetIsAheadIn() {
        // Target: Cryptography L3 and Mass Media L1; the thief already has Mass Media L1.
        when(researchRepository.findByPlayerId(targetId)).thenReturn(List.of(
                tech(ActionType.RESEARCH_CRYPTOGRAPHY, 3), tech(ActionType.RESEARCH_MASS_MEDIA, 1)));
        when(researchGateway.level(actorId, ActionType.RESEARCH_CRYPTOGRAPHY)).thenReturn(1);
        when(researchGateway.level(actorId, ActionType.RESEARCH_MASS_MEDIA)).thenReturn(1);

        resolver(0.0, 0.0).stealTechnology(actorId, targetId);   // success roll, then the pick

        verify(researchGateway).advanceResearch(actorId, ActionType.RESEARCH_CRYPTOGRAPHY);
        verify(researchGateway, never()).advanceResearch(actorId, ActionType.RESEARCH_MASS_MEDIA);
    }

    @Test
    void nothingToStealWhenTheThiefIsNotBehind() {
        when(researchRepository.findByPlayerId(targetId)).thenReturn(List.of(tech(ActionType.RESEARCH_CRYPTOGRAPHY, 2)));
        when(researchGateway.level(actorId, ActionType.RESEARCH_CRYPTOGRAPHY)).thenReturn(2);

        resolver(0.0).stealTechnology(actorId, targetId);

        verify(researchGateway, never()).advanceResearch(any(), any());
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.INVOLVED_ONLY), eq(actorId), eq(targetId),
                argThat(h -> h.contains("found no new technology")), any());
    }
}
