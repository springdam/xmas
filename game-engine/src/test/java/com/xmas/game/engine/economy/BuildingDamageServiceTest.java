package com.xmas.game.engine.economy;

import static org.assertj.core.api.Assertions.assertThat;

import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import org.junit.jupiter.api.Test;

class BuildingDamageServiceTest {

    private PlayerEconomyEntity economy() {
        return PlayerEconomyEntity.builder().farms(10).factories(10).robotFactories(10).banks(10).build();
    }

    @Test
    void sturdyBuildingsSurviveWhereFragileOnesFall() {
        // strength 0.5: farms 0.125, factories 0.3, banks 0.4, robot factories 0.5; every roll is 0.35
        PlayerEconomyEntity economy = economy();
        BuildingDamageService.Losses losses =
                new BuildingDamageService(new EconomyProperties(), () -> 0.35).strike(economy, 0.5);

        assertThat(losses.getFarms()).isZero();
        assertThat(losses.getFactories()).isZero();
        assertThat(losses.getBanks()).isEqualTo(10);
        assertThat(losses.getRobotFactories()).isEqualTo(10);
        assertThat(economy.getBanks()).isZero();
        assertThat(economy.getFarms()).isEqualTo(10);
    }

    @Test
    void chanceIsCappedAtCertainDestruction() {
        PlayerEconomyEntity economy = economy();
        new BuildingDamageService(new EconomyProperties(), () -> 0.99).strike(economy, 5.0);

        assertThat(economy.getRobotFactories()).isZero();   // 5 x 1.0 capped at 1
        assertThat(economy.getFarms()).isZero();            // 5 x 0.25 capped at 1
    }

    @Test
    void describesLossesInPlainEnglish() {
        assertThat(BuildingDamageService.describe(new BuildingDamageService.Losses(0, 2, 1, 1)))
                .isEqualTo("2 factories, 1 bank and 1 robot factory");
        assertThat(BuildingDamageService.describe(new BuildingDamageService.Losses(1, 0, 0, 0)))
                .isEqualTo("1 farm");
        assertThat(BuildingDamageService.describe(new BuildingDamageService.Losses(0, 0, 0, 0)))
                .isEqualTo("no buildings");
    }
}
