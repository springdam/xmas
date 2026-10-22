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
    public int level(UUID playerId, ActionType research) {
        return repository.findByPlayerIdAndResearchType(playerId, research)
                .map(PlayerResearchEntity::getLevel)
                .orElse(0);
    }

    @Override
    @Transactional
    public void advanceResearch(UUID playerId, ActionType research) {
        PlayerResearchEntity entity = repository.findByPlayerIdAndResearchType(playerId, research)
                .orElseGet(() -> PlayerResearchEntity.builder()
                        .id(UUID.randomUUID())
                        .playerId(playerId)
                        .researchType(research)
                        .level(0)
                        .build());
        entity.setLevel(entity.getLevel() + 1);
        repository.save(entity);
    }
}
