package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.clock")
public class GameClockProperties {

    private long tickMillis = 5000;

    private long gameTimeStepMillis = 5000;
}
