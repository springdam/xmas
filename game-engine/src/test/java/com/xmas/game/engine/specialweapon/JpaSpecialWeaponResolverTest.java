package com.xmas.game.engine.specialweapon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.common.domain.Nation;
import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.config.SpecialWeaponProperties;
import com.xmas.game.engine.economy.BuildingDamageService;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.entity.PlayerSpecialWeaponsEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerSpecialWeaponsRepository;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import com.xmas.game.engine.palace.PalaceService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaSpecialWeaponResolverTest {

    @Test
    void undefendedStrikeLandsConsumesWarheadsAndDestroysAssets() {
        UUID attackerId = UUID.randomUUID();
        UUID defenderId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();

        PlayerRepository playerRepository = mock(PlayerRepository.class);
        PlayerSpecialWeaponsRepository specialRepo = mock(PlayerSpecialWeaponsRepository.class);
        PlayerMilitaryRepository militaryRepo = mock(PlayerMilitaryRepository.class);
        PlayerEconomyRepository economyRepo = mock(PlayerEconomyRepository.class);
        GameRepository gameRepo = mock(GameRepository.class);
        NewsService newsService = mock(NewsService.class);
        com.xmas.game.engine.research.ResearchModifierService researchModifierService =
                mock(com.xmas.game.engine.research.ResearchModifierService.class);
        when(researchModifierService.warheadMultiplier(any())).thenReturn(1);
        when(researchModifierService.popularityDamageMultiplier(any())).thenReturn(1.0);
        RandomSource random = () -> 0.99; // never intercepted

        PlayerEntity attacker = PlayerEntity.builder().id(attackerId).gameId(gameId).nation(Nation.USA).build();
        PlayerEntity defender = PlayerEntity.builder().id(defenderId).gameId(gameId).nation(Nation.RUSSIA).build();
        PlayerSpecialWeaponsEntity arsenal = PlayerSpecialWeaponsEntity.builder()
                .playerId(attackerId).nukes(3).espionageSatellites(0).build();
        PlayerMilitaryEntity defenderMil = PlayerMilitaryEntity.builder()
                .playerId(defenderId).infantry(100).tanks(40).missileBatteries(0).airDefenseBatteries(0)
                .stealthBombers(0).specialOps(0).build();
        PlayerEconomyEntity defenderEco = PlayerEconomyEntity.builder()
                .playerId(defenderId).farms(10).factories(10).robotFactories(10).banks(10).build();

        when(playerRepository.findById(attackerId)).thenReturn(Optional.of(attacker));
        when(playerRepository.findById(defenderId)).thenReturn(Optional.of(defender));
        when(playerRepository.findByGameId(gameId)).thenReturn(List.of(attacker, defender));
        when(specialRepo.findById(attackerId)).thenReturn(Optional.of(arsenal));
        when(militaryRepo.findById(defenderId)).thenReturn(Optional.of(defenderMil));
        when(economyRepo.findById(defenderId)).thenReturn(Optional.of(defenderEco));
        when(gameRepo.findById(gameId)).thenReturn(Optional.empty());

        JpaSpecialWeaponResolver resolver = new JpaSpecialWeaponResolver(
                playerRepository, specialRepo, militaryRepo, economyRepo, gameRepo,
                random, new SpecialWeaponProperties(), new PopularityProperties(), researchModifierService, newsService,
                new BuildingDamageService(new EconomyProperties(), () -> 0.5), mock(PalaceService.class));

        resolver.launch(attackerId, defenderId, 2);

        // 2 warheads land: strength min(1, 2 * 0.35) = 0.7. Each building is lost with chance
        // 0.7 x fragility; rolling 0.5, farms (0.175) and factories (0.42) survive while
        // banks (0.56) and robot factories (0.7) are destroyed.
        assertThat(defenderEco.getFarms()).isEqualTo(10);
        assertThat(defenderEco.getFactories()).isEqualTo(10);
        assertThat(defenderEco.getBanks()).isZero();
        assertThat(defenderEco.getRobotFactories()).isZero();
        assertThat(defenderMil.getInfantry()).isEqualTo(30);
        assertThat(arsenal.getNukes()).isEqualTo(1);
        verify(newsService).record(eq(gameId), any(), eq(NewsVisibility.PUBLIC), eq(attackerId), eq(defenderId),
                any(), any());
    }
}
