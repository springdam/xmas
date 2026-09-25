package com.xmas.game.engine.rest;

import com.xmas.game.common.account.AccountDirectory;
import com.xmas.game.common.security.SessionIdentity;
import com.xmas.game.engine.api.GameApi;
import com.xmas.game.engine.api.model.GameResponse;
import com.xmas.game.engine.api.model.PlayerResponse;
import com.xmas.game.engine.api.model.PlayerStateResponse;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.mapper.GameMapper;
import com.xmas.game.engine.service.GameService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GameController implements GameApi {

    private final GameService gameService;
    private final SessionIdentity sessionIdentity;
    private final AccountDirectory accountDirectory;

    @Override
    public ResponseEntity<GameResponse> createGame() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GameMapper.toGameResponse(gameService.createGame()));
    }

    @Override
    public ResponseEntity<GameResponse> getGame(UUID gameId, String xSessionToken) {
        sessionIdentity.requireAccountId(xSessionToken);
        return ResponseEntity.ok(GameMapper.toGameResponse(gameService.getGame(gameId)));
    }

    @Override
    public ResponseEntity<java.util.List<com.xmas.game.engine.api.model.GamePlayerSummary>> getGamePlayers(
            UUID gameId, String xSessionToken) {
        sessionIdentity.requireAccountId(xSessionToken);
        List<PlayerEntity> players = gameService.getPlayers(gameId);
        Map<UUID, String> usernames = accountDirectory.usernames(
                players.stream().map(PlayerEntity::getAccountId).toList());
        return ResponseEntity.ok(players.stream()
                .map(player -> GameMapper.toGamePlayerSummary(player, usernames.get(player.getAccountId())))
                .toList());
    }

    @Override
    public ResponseEntity<PlayerResponse> joinGame(UUID gameId, String xSessionToken) {
        UUID accountId = sessionIdentity.requireAccountId(xSessionToken);
        PlayerEntity player = gameService.joinGame(gameId, accountId);
        return ResponseEntity.status(HttpStatus.CREATED).body(GameMapper.toPlayerResponse(player));
    }

    @Override
    public ResponseEntity<GameResponse> startGame(UUID gameId, String xSessionToken) {
        UUID playerId = sessionIdentity.requirePlayer(xSessionToken);
        return ResponseEntity.ok(GameMapper.toGameResponse(gameService.startGame(gameId, playerId)));
    }

    @Override
    public ResponseEntity<GameResponse> pauseGame(UUID gameId, String xSessionToken) {
        UUID playerId = sessionIdentity.requirePlayer(xSessionToken);
        return ResponseEntity.ok(GameMapper.toGameResponse(gameService.pauseGame(gameId, playerId)));
    }

    @Override
    public ResponseEntity<GameResponse> resumeGame(UUID gameId, String xSessionToken) {
        UUID playerId = sessionIdentity.requirePlayer(xSessionToken);
        return ResponseEntity.ok(GameMapper.toGameResponse(gameService.resumeGame(gameId, playerId)));
    }

    @Override
    public ResponseEntity<PlayerStateResponse> getPlayerState(UUID playerId, String xSessionToken) {
        UUID viewerId = sessionIdentity.requirePlayer(xSessionToken);
        PlayerEntity player = gameService.getPlayer(playerId);
        boolean fullView = viewerId.equals(playerId) || gameService.hasSatelliteIntel(viewerId);
        if (!fullView) {
            return ResponseEntity.ok(GameMapper.toLimitedPlayerState(player, gameService.getRelations(playerId)));
        }
        return ResponseEntity.ok(GameMapper.toPlayerState(
                player,
                gameService.getEconomy(playerId),
                gameService.getMilitary(playerId),
                gameService.getEspionage(playerId),
                gameService.getSpecialWeapons(playerId),
                gameService.getProtection(playerId),
                gameService.getResearch(playerId),
                gameService.getRelations(playerId)));
    }
}
