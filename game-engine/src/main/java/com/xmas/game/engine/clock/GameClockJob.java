package com.xmas.game.engine.clock;

import com.xmas.game.engine.service.GameTickService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class GameClockJob implements Job {

    private final GameTickService gameTickService;

    @Override
    public void execute(JobExecutionContext context) {
        try {
            gameTickService.tick();
        } catch (RuntimeException ex) {
            log.error("Game tick failed", ex);
        }
    }
}
