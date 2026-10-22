package com.xmas.game.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.nations")
public class NationProperties {

    /** What every nation starts with. All nations are equal; the nation is only a flag and a name. */
    private Start start = new Start();

    @Getter
    @Setter
    public static class Start {
        private long startingCash = 350_000;
        private int farms = 8;
        private int factories = 7;
        private int robotFactories = 2;
        private int banks = 4;
        /** Land plots: the most farms, factories, robot factories and banks the nation can hold. */
        private int land = 40;
    }
}
