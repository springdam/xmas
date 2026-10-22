package com.xmas.game.engine.protection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xmas.game.common.domain.Nation;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerProtectionEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerProtectionRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.service.NewsService;
import com.xmas.game.engine.palace.PalaceService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaderKillServiceTest {

    private PlayerRepository playerRepository;
    private PlayerProtectionRepository protectionRepository;
    private LeaderKillService service;

    private final UUID attackerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private PlayerEntity attacker;
    private PlayerEntity target;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        protectionRepository = mock(PlayerProtectionRepository.class);
        GameRepository gameRepository = mock(GameRepository.class);
        NewsService newsService = mock(NewsService.class);
        service = new LeaderKillService(playerRepository, protectionRepository, gameRepository, newsService,
                mock(PalaceService.class));
        attacker = PlayerEntity.builder().id(attackerId).gameId(UUID.randomUUID()).nation(Nation.USA).build();
        target = PlayerEntity.builder().id(targetId).gameId(UUID.randomUUID()).nation(Nation.RUSSIA)
                .eliminated(false).build();
    }

    @Test
    void doppelgangerAbsorbsTheKill() {
        PlayerProtectionEntity protection = PlayerProtectionEntity.builder()
                .playerId(targetId).doublegangers(2).build();
        when(protectionRepository.findById(targetId)).thenReturn(Optional.of(protection));

        boolean killed = service.attemptKill(attacker, target, NewsType.ESPIONAGE, "assassination");

        assertThat(killed).isFalse();
        assertThat(protection.getDoublegangers()).isEqualTo(1);
        assertThat(target.isEliminated()).isFalse();
    }

    @Test
    void withoutDoppelgangerTheNationIsEliminated() {
        PlayerProtectionEntity protection = PlayerProtectionEntity.builder()
                .playerId(targetId).doublegangers(0).build();
        when(protectionRepository.findById(targetId)).thenReturn(Optional.of(protection));

        boolean killed = service.attemptKill(attacker, target, NewsType.ATTACK, "decapitation strike");

        assertThat(killed).isTrue();
        assertThat(target.isEliminated()).isTrue();
    }
}
