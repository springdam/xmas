package com.xmas.game.combat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.xmas.game.combat")
@EnableConfigurationProperties(CombatProperties.class)
public class CombatConfiguration {
}
