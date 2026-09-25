package com.xmas.game.actions.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.xmas.game.actions")
@EnableConfigurationProperties(ActionProperties.class)
public class ActionConfiguration {
}
