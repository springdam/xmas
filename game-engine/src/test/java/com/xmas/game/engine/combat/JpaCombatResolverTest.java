package com.xmas.game.engine.combat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.combat.config.CombatProperties;
import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.combat.service.CombatCalculator;
import com.xmas.game.common.domain.Nation;
import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.config.SpecialForcesProperties;
import com.xmas.game.engine.economy.BuildingDamageService;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.protection.LeaderKillService;
import com.xmas.game.engine.protection.ProtectionService;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.research.ResearchModifierService;
import com.xmas.game.newsfeed.service.NewsService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaCombatResolverTest {

    private final UUID gameId = UUID.randomUUID();
    private final UUID attackerId = UUID.randomUUID();
    private final UUID defenderId = UUID.randomUUID();

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final PlayerMilitaryRepository militaryRepository = mock(PlayerMilitaryRepository.class);
    private final ProtectionService protectionService = mock(ProtectionService.class);
    private final LeaderKillService leaderKillService = mock(LeaderKillService.class);
    private final PlayerEconomyRepository economyRepository = mock(PlayerEconomyRepository.class);
    private final PlayerEconomyEntity defenderEco = PlayerEconomyEntity.builder()
            .playerId(defenderId).farms(5).factories(5).robotFactories(5).banks(5).build();

    private final PlayerEntity attacker = PlayerEntity.builder()
            .id(attackerId).gameId(gameId).nation(Nation.USA).popularity(50).build();
    private final PlayerEntity defender = PlayerEntity.builder()
            .id(defenderId).gameId(gameId).nation(Nation.DENMARK).popularity(50).build();

    /** A resolver whose dice always come up {@code roll}; the attacker fields {@code infantry}, the defender nothing. */
    private JpaCombatResolver resolver(double roll, int infantry) {
        return resolver(roll, infantry, 0, 0.99);
    }

    /** As above, with a defending army and the roll each building makes against destruction. */
    private JpaCombatResolver resolver(double roll, int infantry, int defenderInfantry, double buildingRoll) {
        RandomSource random = () -> roll;
        when(economyRepository.findById(defenderId)).thenReturn(Optional.of(defenderEco));
        when(playerRepository.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(playerRepository.findById(defenderId)).thenReturn(Optional.of(defender));
        when(militaryRepository.findById(attackerId)).thenReturn(Optional.of(PlayerMilitaryEntity.builder()
                .playerId(attackerId).infantry(infantry).build()));
        when(militaryRepository.findById(defenderId)).thenReturn(Optional.of(PlayerMilitaryEntity.builder()
                .playerId(defenderId).infantry(defenderInfantry).build()));

        GameRepository gameRepository = mock(GameRepository.class);
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());
        ResearchModifierService modifiers = mock(ResearchModifierService.class);
        when(modifiers.combatMultiplier(any())).thenReturn(1.0);
        when(modifiers.combatDefenseMultiplier(any())).thenReturn(1.0);
        when(modifiers.popularityDamageMultiplier(any())).thenReturn(1.0);
        when(modifiers.airSuperiorityMultiplier(any())).thenReturn(1.0);

        return new JpaCombatResolver(playerRepository, militaryRepository, gameRepository,
                new CombatCalculator(new CombatProperties(), random), modifiers, protectionService,
                leaderKillService, new PopularityProperties(), new SpecialForcesProperties(),
                mock(NewsService.class), economyRepository,
                new BuildingDamageService(new EconomyProperties(), () -> buildingRoll), new EconomyProperties());
    }

    @Test
    void repeatedDefeats_costLessPopularityEachTime() {
        JpaCombatResolver resolver = resolver(0.5, 50);

        resolver.resolveAttack(attackerId, defenderId, 1);
        assertThat(defender.getPopularity()).isEqualTo(45);   // -5
        resolver.resolveAttack(attackerId, defenderId, 1);
        assertThat(defender.getPopularity()).isEqualTo(41);   // -5*0.7 = -3.5 -> -4
        resolver.resolveAttack(attackerId, defenderId, 1);
        assertThat(defender.getPopularity()).isEqualTo(39);   // -5*0.49 = -2.45 -> -2
        assertThat(defender.getWarStrain()).isEqualTo(3);
    }

    @Test
    void decapitation_groundForcesHaveDiminishingReturns() {
        defender.setPopularity(10);
        // 100 ground units = 0.5 * sqrt(100) = 5 strike power vs 0 protection + 5 resistance: 50%.
        JpaCombatResolver resolver = resolver(0.5, 100);

        resolver.resolveDecapitationStrike(attackerId, defenderId);

        verify(leaderKillService, never()).attemptKill(any(), any(), any(), anyString());
    }

    @Test
    void decapitation_aBigEnoughArmyCanStillReachTheLeader() {
        defender.setPopularity(10);
        // 400 ground units = 10 strike power: 67%.
        JpaCombatResolver resolver = resolver(0.5, 400);

        resolver.resolveDecapitationStrike(attackerId, defenderId);

        verify(leaderKillService).attemptKill(eq(attacker), eq(defender), any(), anyString());
    }

    @Test
    void aBeatenDefenderLosesTheMostFragileBuildingsFirst() {
        // Undefended: attacker share 1, roll 0.5 -> 60% of forces lost -> strike strength 0.6 x 0.3 = 0.18.
        // Each building rolls 0.15: robot factories (0.18) fall; banks (0.144), factories (0.108)
        // and farms (0.009) stand.
        resolver(0.5, 50, 0, 0.15).resolveAttack(attackerId, defenderId, 1);

        assertThat(defenderEco.getRobotFactories()).isZero();
        assertThat(defenderEco.getBanks()).isEqualTo(5);
        assertThat(defenderEco.getFactories()).isEqualTo(5);
        assertThat(defenderEco.getFarms()).isEqualTo(5);
    }

    @Test
    void aCloserDefeatCostsFewerBuildings() {
        // 50 infantry vs 10 defending: attacker share 50/62 = 0.81, still a win at roll 0.5, but only
        // 48% of forces lost -> strength 0.145. With the same 0.15 building roll as the rout above,
        // the robot factories (0.145) now survive.
        resolver(0.5, 50, 10, 0.15).resolveAttack(attackerId, defenderId, 1);

        assertThat(defenderEco.getRobotFactories()).isEqualTo(5);
        assertThat(defenderEco.getBanks()).isEqualTo(5);
    }

    @Test
    void aDefenderWhoHoldsLosesNoBuildings() {
        // 1 infantry against 100: the attack is repelled.
        resolver(0.5, 1, 100, 0.0).resolveAttack(attackerId, defenderId, 1);

        assertThat(defenderEco.getRobotFactories()).isEqualTo(5);
        assertThat(defenderEco.getFarms()).isEqualTo(5);
    }
}
