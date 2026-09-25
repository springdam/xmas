package com.xmas.game.engine.config;

import com.xmas.game.common.domain.Nation;
import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.nations")
public class NationProperties {

    private Map<Nation, Start> start = new EnumMap<>(Nation.class);

    @Getter
    @Setter
    public static class Start {
        private long startingCash;
        private int farms;
        private int factories;
        private int robotFactories;
        private int banks;
    }
}
