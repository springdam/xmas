package com.xmas.game.engine.clock;

import com.xmas.game.engine.config.GameClockProperties;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameClockSchedule {

    @Bean
    public JobDetail gameTickJobDetail() {
        return JobBuilder.newJob(GameClockJob.class)
                .withIdentity("gameTickJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger gameTickTrigger(JobDetail gameTickJobDetail, GameClockProperties properties) {
        return TriggerBuilder.newTrigger()
                .forJob(gameTickJobDetail)
                .withIdentity("gameTickTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(properties.getTickMillis())
                        .repeatForever())
                .build();
    }
}
