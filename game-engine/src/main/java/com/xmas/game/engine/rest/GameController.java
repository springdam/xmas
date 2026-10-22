package com.xmas.game.engine.rest;

import com.xmas.game.common.account.AccountDirectory;
import com.xmas.game.common.error.ForbiddenException;
import com.xmas.game.common.security.SessionIdentity;
import com.xmas.game.engine.api.GameApi;
import com.xmas.game.engine.api.model.GameListing;
import com.xmas.game.engine.api.model.ChoosePalaceStyleRequest;
import com.xmas.game.engine.api.model.GameResponse;
import com.xmas.game.engine.api.model.PalaceResponse;
import com.xmas.game.engine.api.model.UpgradePalaceRequest;
import com.xmas.game.engine.api.model.PlayerResponse;
import com.xmas.game.engine.api.model.PlayerStateResponse;
import com.xmas.game.engine.config.EconomyProperties;
import com.xmas.game.engine.diplomacy.PopularityBoostService;
import com.xmas.game.engine.economy.LandService;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.entity.PlayerPalaceEntity;
import com.xmas.game.engine.military.UpkeepService;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.domain.PalacePart;
import com.xmas.game.engine.domain.PalaceStyle;
import com.xmas.game.engine.mapper.GameMapper;
import com.xmas.game.engine.palace.PalaceService;
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
    private final LandService landService;
    private final EconomyProperties economyProperties;
    private final UpkeepService upkeepService;
    private final PopularityBoostService popularityBoostService;
    private final PalaceService palaceService;

    @Override
    public ResponseEntity<GameResponse> createGame() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GameMapper.toGameResponse(gameService.createGame()));
    }

    @Override
    public ResponseEntity<List<GameListing>> listGames(String xSessionToken) {
        UUID accountId = sessionIdentity.requireAccountId(xSessionToken);
        List<GameService.Listing> listings = gameService.listGames(accountId);
        Map<UUID, String> usernames = accountDirectory.usernames(listings.stream()
                .flatMap(listing -> listing.players().stream())
                .filter(PlayerEntity::isAdmin)
                .map(PlayerEntity::getAccountId)
                .toList());
        return ResponseEntity.ok(listings.stream()
                .map(listing -> GameMapper.toGameListing(listing, listing.players().stream()
                        .filter(PlayerEntity::isAdmin)
                        .findFirst()
                        .map(admin -> usernames.get(admin.getAccountId()))
                        .orElse(null)))
                .toList());
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
                .map(player -> GameMapper.toGamePlayerSummary(player, usernames.get(player.getAccountId()))
                        .palaceValue(palaceService.value(player.getId())))
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
        UUID playerId = sessionIdentity.requirePlayer(xSessionToken, gameId);
        return ResponseEntity.ok(GameMapper.toGameResponse(gameService.startGame(gameId, playerId)));
    }

    @Override
    public ResponseEntity<GameResponse> pauseGame(UUID gameId, String xSessionToken) {
        UUID playerId = sessionIdentity.requirePlayer(xSessionToken, gameId);
        return ResponseEntity.ok(GameMapper.toGameResponse(gameService.pauseGame(gameId, playerId)));
    }

    @Override
    public ResponseEntity<GameResponse> resumeGame(UUID gameId, String xSessionToken) {
        UUID playerId = sessionIdentity.requirePlayer(xSessionToken, gameId);
        return ResponseEntity.ok(GameMapper.toGameResponse(gameService.resumeGame(gameId, playerId)));
    }

    @Override
    public ResponseEntity<PlayerStateResponse> getPlayerState(UUID playerId, String xSessionToken) {
        PlayerEntity player = gameService.getPlayer(playerId);
        UUID viewerId = sessionIdentity.requirePlayer(xSessionToken, player.getGameId());
        boolean fullView = viewerId.equals(playerId) || gameService.hasSatelliteIntel(viewerId);
        // Palaces are public: everyone sees everyone's, but only you see your unspent tokens.
        PlayerPalaceEntity palaceEntity = palaceService.palace(playerId);
        PalaceResponse palace = GameMapper.toPalaceResponse(palaceEntity, palaceService.value(palaceEntity),
                viewerId.equals(playerId), palaceService.tierCosts());
        if (!fullView) {
            return ResponseEntity.ok(GameMapper.toLimitedPlayerState(player, gameService.getRelations(playerId))
                    .palace(palace));
        }
        PlayerEconomyEntity economy = gameService.getEconomy(playerId);
        PlayerMilitaryEntity military = gameService.getMilitary(playerId);
        PlayerStateResponse state = GameMapper.toPlayerState(
                player,
                economy,
                landService.capacity(playerId),
                landService.inUse(playerId),
                economyProperties.industryCapacity(economy.getFarms()),
                military,
                upkeepService.perTick(military),
                gameService.getEspionage(playerId),
                gameService.getSpecialWeapons(playerId),
                gameService.getProtection(playerId),
                gameService.getResearch(playerId),
                gameService.getRelations(playerId));
        state.palace(palace);
        if (viewerId.equals(playerId)) {
            // Your own parade outlook; rivals (even with satellites) don't see it.
            state.getPlayer()
                    .nextParadeGain(popularityBoostService.nextParadeGain(player))
                    .paradeReadyAtTick(popularityBoostService.paradeReadyAtTick(player));
        }
        return ResponseEntity.ok(state);
    }

    @Override
    public ResponseEntity<PalaceResponse> upgradePalace(UUID playerId, String xSessionToken,
                                                        UpgradePalaceRequest request) {
        PlayerEntity player = gameService.getPlayer(playerId);
        if (!sessionIdentity.requirePlayer(xSessionToken, player.getGameId()).equals(playerId)) {
            throw new ForbiddenException("You may only improve your own palace");
        }
        boolean cash = request.getPayWith() == UpgradePalaceRequest.PayWithEnum.CASH;
        PlayerPalaceEntity palace = palaceService.upgrade(playerId, PalacePart.valueOf(request.getPart().name()), cash);
        return ResponseEntity.ok(GameMapper.toPalaceResponse(palace, palaceService.value(palace), true,
                palaceService.tierCosts()));
    }

    @Override
    public ResponseEntity<PalaceResponse> choosePalaceStyle(UUID playerId, String xSessionToken,
                                                            ChoosePalaceStyleRequest request) {
        PlayerEntity player = gameService.getPlayer(playerId);
        if (!sessionIdentity.requirePlayer(xSessionToken, player.getGameId()).equals(playerId)) {
            throw new ForbiddenException("You may only choose your own palace");
        }
        PlayerPalaceEntity palace = palaceService.chooseStyle(playerId, PalaceStyle.valueOf(request.getStyle().name()));
        return ResponseEntity.ok(GameMapper.toPalaceResponse(palace, palaceService.value(palace), true,
                palaceService.tierCosts()));
    }
}
