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
import com.xmas.game.engine.config.EspionageProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.config.SpecialForcesProperties;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerEspionageEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.protection.LeaderKillService;
import com.xmas.game.engine.protection.ProtectionService;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerEspionageRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
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

    private final PlayerEspionageEntity actorEsp = PlayerEspionageEntity.builder().playerId(actorId).spies(10).counterIntel(0).build();
    private final PlayerEspionageEntity targetEsp = PlayerEspionageEntity.builder().playerId(targetId).spies(0).counterIntel(10).build();
    private final PlayerEconomyEntity targetEco = PlayerEconomyEntity.builder()
            .playerId(targetId).farms(5).factories(10).robotFactories(5).banks(5).build();

    /** A resolver whose dice come up in the given order. */
    private JpaEspionageResolver resolver(double... rolls) {
        int[] next = {0};
        RandomSource random = () -> rolls[next[0]++];

        PlayerEntity actor = PlayerEntity.builder().id(actorId).gameId(gameId).nation(Nation.RUSSIA).popularity(50).build();
        PlayerEntity target = PlayerEntity.builder().id(targetId).gameId(gameId).nation(Nation.DENMARK).popularity(50).build();
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
                militaryRepository, mock(PlayerResearchRepository.class), gameRepository,
                random, new EspionageProperties(), new PopularityProperties(), modifiers,
                mock(ResearchGateway.class), mock(ProtectionService.class), mock(LeaderKillService.class),
                new SpecialForcesProperties(), newsService);
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
        assertThat(targetEco.getFactories()).isEqualTo(8);       // 20% of 10
        assertThat(targetEco.getRobotFactories()).isEqualTo(4);  // 20% of 5
        verify(espionageRepository, never()).save(any());
    }

    @Test
    void successfulReconIsSecretAndCostsNoSpies() {
        resolver(0.0, 0.5, 0.5, 0.5, 0.5, 0.5).gatherIntelligence(actorId, targetId);

        assertThat(actorEsp.getSpies()).isEqualTo(10);
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.ACTOR_ONLY), eq(actorId), eq(targetId),
                argThat(h -> h.startsWith("INTEL on DENMARK")), any());
        verify(newsService, never()).record(any(), any(), eq(NewsVisibility.INVOLVED_ONLY), any(), any(), any(), any());
    }
}
