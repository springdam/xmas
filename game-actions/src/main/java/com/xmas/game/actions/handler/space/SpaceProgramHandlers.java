package com.xmas.game.actions.handler.space;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.handler.ActionHandler;
import com.xmas.game.actions.space.SpaceGateway;
import com.xmas.game.actions.state.PlayerStateGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One handler per spaceship part; they all share SpaceProjectHandler. */
@Configuration
public class SpaceProgramHandlers {

    @Bean
    ActionHandler spaceLaunchTowerHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_LAUNCH_TOWER, state, space);
    }

    @Bean
    ActionHandler spaceEnginesHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_ENGINES, state, space);
    }

    @Bean
    ActionHandler spaceFirstStageHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_FIRST_STAGE, state, space);
    }

    @Bean
    ActionHandler spaceSecondStageHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_SECOND_STAGE, state, space);
    }

    @Bean
    ActionHandler spaceThirdStageHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_THIRD_STAGE, state, space);
    }

    @Bean
    ActionHandler spacePrimaryBoostersHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_PRIMARY_BOOSTERS, state, space);
    }

    @Bean
    ActionHandler spaceSecondaryBoostersHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_SECONDARY_BOOSTERS, state, space);
    }

    @Bean
    ActionHandler spaceCommandModuleHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_COMMAND_MODULE, state, space);
    }

    @Bean
    ActionHandler spaceLandingModuleHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_LANDING_MODULE, state, space);
    }

    @Bean
    ActionHandler spaceSupportModuleHandler(PlayerStateGateway state, SpaceGateway space) {
        return new SpaceProjectHandler(ActionType.SPACE_SUPPORT_MODULE, state, space);
    }
}
