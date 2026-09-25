package com.xmas.game.engine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("com.xmas.game.engine")
@EnableJpaRepositories("com.xmas.game.engine.repository")
@EntityScan("com.xmas.game.engine.entity")
@EnableConfigurationProperties({
        NationProperties.class,
        EconomyProperties.class,
        GameClockProperties.class,
        EspionageProperties.class,
        SpecialWeaponProperties.class,
        ResearchProperties.class,
        PopularityProperties.class,
        SpecialForcesProperties.class
})
public class EngineConfiguration {
}
