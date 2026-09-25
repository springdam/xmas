package com.xmas.game.app.game;

import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.common.game.PlayerDirectory;
import com.xmas.game.engine.repository.PlayerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlayerDirectoryService implements PlayerDirectory {

    private final PlayerRepository playerRepository;

    @Override
    @Transactional(readOnly = true)
    public UUID requireGameId(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found: " + playerId))
                .getGameId();
    }
}
