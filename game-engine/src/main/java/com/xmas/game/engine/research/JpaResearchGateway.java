package com.xmas.game.engine.research;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.research.ResearchGateway;
import com.xmas.game.engine.entity.PlayerResearchEntity;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaResearchGateway implements ResearchGateway {

    private final PlayerResearchRepository repository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasResearch(UUID playerId, ActionType research) {
        return repository.existsByPlayerIdAndResearchType(playerId, research);
    }

    @Override
    @Transactional
    public void addResearch(UUID playerId, ActionType research) {
        if (repository.existsByPlayerIdAndResearchType(playerId, research)) {
            return;
        }
        repository.save(PlayerResearchEntity.builder()
                .id(UUID.randomUUID())
                .playerId(playerId)
                .researchType(research)
                .build());
    }
}
