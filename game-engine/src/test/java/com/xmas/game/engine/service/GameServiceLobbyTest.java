package com.xmas.game.engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xmas.game.common.domain.Nation;
import com.xmas.game.common.error.BadRequestException;
import com.xmas.game.engine.config.GameClockProperties;
import com.xmas.game.engine.config.NationProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.domain.GameStatus;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerEspionageRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerProtectionRepository;
import com.xmas.game.engine.repository.PlayerRelationRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import com.xmas.game.engine.repository.PlayerSpecialWeaponsRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GameServiceLobbyTest {

    private final GameRepository gameRepository = mock(GameRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final GameService service = new GameService(gameRepository, playerRepository,
            mock(PlayerEconomyRepository.class), mock(PlayerMilitaryRepository.class),
            mock(PlayerEspionageRepository.class), mock(PlayerSpecialWeaponsRepository.class),
            mock(PlayerProtectionRepository.class), mock(PlayerResearchRepository.class),
            mock(PlayerRelationRepository.class), new NationProperties(), new GameClockProperties(),
            new PopularityProperties());

    private final UUID me = UUID.randomUUID();

    private GameEntity game(GameStatus status, String createdAt) {
        return GameEntity.builder().id(UUID.randomUUID()).status(status)
                .createdAt(Instant.parse(createdAt)).gameTime(Instant.parse(createdAt)).build();
    }

    private PlayerEntity player(GameEntity game, UUID accountId, boolean admin) {
        return PlayerEntity.builder().id(UUID.randomUUID()).gameId(game.getId()).accountId(accountId)
                .admin(admin).build();
    }

    @Test
    void listsAllLobbiesNewestFirstPlusStartedGamesImIn() {
        GameEntity olderLobby = game(GameStatus.LOBBY, "2026-10-05T10:00:00Z");
        GameEntity newerLobby = game(GameStatus.LOBBY, "2026-10-05T11:00:00Z");
        GameEntity myRunning = game(GameStatus.RUNNING, "2026-10-05T09:00:00Z");
        PlayerEntity myPlayer = player(myRunning, me, false);
        PlayerEntity host = player(newerLobby, UUID.randomUUID(), true);

        when(playerRepository.findByAccountId(me)).thenReturn(List.of(myPlayer));
        when(gameRepository.findByStatus(GameStatus.LOBBY)).thenReturn(List.of(olderLobby, newerLobby));
        when(gameRepository.findAllById(any())).thenReturn(List.of(myRunning));
        when(playerRepository.findByGameIdIn(anyCollection())).thenReturn(List.of(myPlayer, host));

        List<GameService.Listing> listings = service.listGames(me);

        assertThat(listings).extracting(GameService.Listing::game)
                .containsExactly(newerLobby, olderLobby, myRunning);
        assertThat(listings.get(0).players()).containsExactly(host);
        assertThat(listings.get(0).joined()).isFalse();
        assertThat(listings.get(2).joined()).isTrue();
    }

    @Test
    void aStartedGameImNotInIsNotListed() {
        when(playerRepository.findByAccountId(me)).thenReturn(List.of());
        when(gameRepository.findByStatus(GameStatus.LOBBY)).thenReturn(List.of());
        when(gameRepository.findAllById(any())).thenReturn(List.of());
        when(playerRepository.findByGameIdIn(anyCollection())).thenReturn(List.of());

        assertThat(service.listGames(me)).isEmpty();
    }

    @Test
    void joiningAFullLobbyIsRefused() {
        GameEntity lobby = game(GameStatus.LOBBY, "2026-10-05T10:00:00Z");
        List<PlayerEntity> players = new ArrayList<>();
        players.add(player(lobby, UUID.randomUUID(), true));
        IntStream.range(0, Nation.values().length)
                .forEach(i -> players.add(player(lobby, UUID.randomUUID(), false)));
        when(gameRepository.findById(lobby.getId())).thenReturn(Optional.of(lobby));
        when(playerRepository.findByGameId(lobby.getId())).thenReturn(players);

        assertThatThrownBy(() -> service.joinGame(lobby.getId(), me))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Game is full");
    }
}
