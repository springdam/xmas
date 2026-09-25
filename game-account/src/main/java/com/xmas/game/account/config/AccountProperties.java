package com.xmas.game.account.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.account")
public class AccountProperties {

    private long sessionTtlMinutes = 720;
}
