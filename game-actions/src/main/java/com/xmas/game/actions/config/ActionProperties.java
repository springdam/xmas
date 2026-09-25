package com.xmas.game.actions.config;

import com.xmas.game.actions.domain.ActionType;
import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.actions")
public class ActionProperties {

    private Map<ActionType, Definition> definitions = new EnumMap<>(ActionType.class);

    @Getter
    @Setter
    public static class Definition {
        private long costCash;
        private long durationMillis;
        private boolean targeted;
        private boolean publicVisibility;
        private String description = "";
        private Integer maxTotal;
        private ActionType requiredResearch;
    }
}
