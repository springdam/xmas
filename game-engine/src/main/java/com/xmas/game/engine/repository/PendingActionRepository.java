package com.xmas.game.engine.repository;

import com.xmas.game.engine.domain.PendingActionStatus;
import com.xmas.game.engine.entity.PendingActionEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingActionRepository extends JpaRepository<PendingActionEntity, UUID> {

    List<PendingActionEntity> findByStatusAndCompletesAtLessThanEqualOrderByCompletesAt(
            PendingActionStatus status, Instant threshold);
}
