package com.xmas.game.app;

import com.xmas.game.account.config.AccountConfiguration;
import com.xmas.game.actions.config.ActionConfiguration;
import com.xmas.game.combat.config.CombatConfiguration;
import com.xmas.game.engine.config.EngineConfiguration;
import com.xmas.game.newsfeed.config.NewsfeedConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "com.xmas.game.app")
@Import({
        AccountConfiguration.class,
        ActionConfiguration.class,
        CombatConfiguration.class,
        NewsfeedConfiguration.class,
        EngineConfiguration.class
})
public class XmasGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(XmasGameApplication.class, args);
    }
}
