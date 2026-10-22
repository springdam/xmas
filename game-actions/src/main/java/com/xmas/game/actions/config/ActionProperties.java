package com.xmas.game.actions.config;

import com.xmas.game.actions.domain.ActionType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "game.actions")
public class ActionProperties {

    private Map<ActionType, Definition> definitions = new EnumMap<>(ActionType.class);

    /** Researching level n costs cost x researchLevelCostFactors[n-1] and takes duration x the duration factor. */
    private List<Double> researchLevelCostFactors = List.of(1.0, 2.0, 3.0);
    private List<Double> researchLevelDurationFactors = List.of(1.0, 1.5, 2.0);

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
        /** Research only: how many levels the tech has (1 = a one-off unlock). */
        private int maxLevel = 1;
    }
}
