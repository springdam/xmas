package com.xmas.game.engine.state;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.queue.PendingActionQueue;
import com.xmas.game.engine.domain.PendingActionStatus;
import com.xmas.game.engine.entity.PendingActionEntity;
import com.xmas.game.engine.repository.PendingActionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaPendingActionQueue implements PendingActionQueue {

    private final PendingActionRepository repository;

    @Override
    @Transactional
    public void enqueue(ActionType type, UUID actorPlayerId, UUID targetPlayerId, int quantity, Instant completesAt) {
        repository.save(PendingActionEntity.builder()
                .id(UUID.randomUUID())
                .actorPlayerId(actorPlayerId)
                .targetPlayerId(targetPlayerId)
                .actionType(type)
                .quantity(quantity)
                .startedAt(Instant.now())
                .completesAt(completesAt)
                .status(PendingActionStatus.PENDING)
                .build());
    }
}
