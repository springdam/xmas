package com.xmas.game.newsfeed.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("com.xmas.game.newsfeed")
@EnableJpaRepositories("com.xmas.game.newsfeed.repository")
@EntityScan("com.xmas.game.newsfeed.entity")
public class NewsfeedConfiguration {
}
