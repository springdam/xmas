package com.xmas.game.engine.service;

import com.xmas.game.common.domain.Nation;
import com.xmas.game.common.error.BadRequestException;
import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.config.NationProperties;
import com.xmas.game.engine.config.GameClockProperties;
import com.xmas.game.engine.config.PopularityProperties;
import com.xmas.game.engine.domain.GameStatus;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerEspionageEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.entity.PlayerProtectionEntity;
import com.xmas.game.engine.entity.PlayerResearchEntity;
import com.xmas.game.engine.entity.PlayerSpecialWeaponsEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerEspionageRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.entity.PlayerRelationEntity;
import com.xmas.game.engine.repository.PlayerProtectionRepository;
import com.xmas.game.engine.repository.PlayerRelationRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerResearchRepository;
import com.xmas.game.engine.repository.PlayerSpecialWeaponsRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final PlayerEconomyRepository economyRepository;
    private final PlayerMilitaryRepository militaryRepository;
    private final PlayerEspionageRepository espionageRepository;
    private final PlayerSpecialWeaponsRepository specialWeaponsRepository;
    private final PlayerProtectionRepository protectionRepository;
    private final PlayerResearchRepository researchRepository;
    private final PlayerRelationRepository relationRepository;
    private final NationProperties nationProperties;
    private final GameClockProperties clockProperties;
    private final PopularityProperties popularityProperties;

    @Transactional
    public GameEntity createGame() {
        Instant now = Instant.now();
        GameEntity game = GameEntity.builder()
                .id(UUID.randomUUID())
                .status(GameStatus.LOBBY)
                .currentTick(0)
                .gameTime(now)
                .tickDurationMillis(clockProperties.getGameTimeStepMillis())
                .createdAt(now)
                .build();
        return gameRepository.save(game);
    }

    /** A game in the lobby list: the game, its players, and whether the caller is one of them. */
    public record Listing(GameEntity game, List<PlayerEntity> players, boolean joined) {
    }

    /** Every game still in the lobby (open to anyone), plus started games the account is already in. */
    @Transactional(readOnly = true)
    public List<Listing> listGames(UUID accountId) {
        Set<UUID> mine = playerRepository.findByAccountId(accountId).stream()
                .map(PlayerEntity::getGameId)
                .collect(Collectors.toSet());
        List<GameEntity> games = new ArrayList<>(gameRepository.findByStatus(GameStatus.LOBBY));
        gameRepository.findAllById(mine).stream()
                .filter(game -> game.getStatus() == GameStatus.RUNNING || game.getStatus() == GameStatus.PAUSED)
                .forEach(games::add);
        Map<UUID, List<PlayerEntity>> players = playerRepository
                .findByGameIdIn(games.stream().map(GameEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(PlayerEntity::getGameId));
        return games.stream()
                .sorted(Comparator.comparing(GameEntity::getCreatedAt).reversed())
                .map(game -> new Listing(game, players.getOrDefault(game.getId(), List.of()),
                        mine.contains(game.getId())))
                .toList();
    }

    @Transactional
    public PlayerEntity joinGame(UUID gameId, UUID accountId) {
        GameEntity game = requireGame(gameId);
        if (game.getStatus() != GameStatus.LOBBY) {
            throw new BadRequestException("Game is no longer accepting players");
        }
        if (playerRepository.existsByGameIdAndAccountId(gameId, accountId)) {
            throw new BadRequestException("Account already joined this game");
        }
        List<PlayerEntity> joined = playerRepository.findByGameId(gameId);
        boolean firstPlayer = joined.isEmpty();
        // The first player is the admin; everyone after needs a nation of their own.
        if (!firstPlayer && joined.stream().filter(p -> !p.isAdmin()).count() >= Nation.values().length) {
            throw new BadRequestException("Game is full");
        }
        PlayerEntity player = PlayerEntity.builder()
                .id(UUID.randomUUID())
                .gameId(gameId)
                .accountId(accountId)
                .nation(null)
                .cash(0)
                .popularity(0)
                .admin(firstPlayer)
                .eliminated(false)
                .joinedAt(Instant.now())
                .build();
        return playerRepository.save(player);
    }

    @Transactional
    public GameEntity startGame(UUID gameId, UUID requestingPlayerId) {
        GameEntity game = requireGame(gameId);
        requireAdmin(gameId, requestingPlayerId);
        if (game.getStatus() != GameStatus.LOBBY) {
            throw new BadRequestException("Game already started");
        }
        List<PlayerEntity> players = playerRepository.findByGameId(gameId).stream()
                .filter(player -> !player.isAdmin())
                .toList();
        if (players.isEmpty()) {
            throw new BadRequestException("Cannot start a game with no playing nations");
        }
        List<Nation> nations = new ArrayList<>(List.of(Nation.values()));
        if (players.size() > nations.size()) {
            throw new BadRequestException("Too many players for available nations");
        }
        Collections.shuffle(nations);

        for (int i = 0; i < players.size(); i++) {
            PlayerEntity player = players.get(i);
            Nation nation = nations.get(i);
            NationProperties.Start start = nationProperties.getStart();
            player.setNation(nation);
            player.setCash(start.getStartingCash());
            player.setPopularity(popularityProperties.getStartingPopularity());
            playerRepository.save(player);
            economyRepository.save(PlayerEconomyEntity.builder()
                    .playerId(player.getId())
                    .farms(start.getFarms())
                    .factories(start.getFactories())
                    .robotFactories(start.getRobotFactories())
                    .banks(start.getBanks())
                    .build());
            seedSupportState(player.getId());
        }

        // The admin is a non-playing organizer: no nation, empty state so reads still work.
        playerRepository.findByGameId(gameId).stream()
                .filter(PlayerEntity::isAdmin)
                .forEach(admin -> {
                    economyRepository.save(PlayerEconomyEntity.builder()
                            .playerId(admin.getId()).farms(0).factories(0).robotFactories(0).banks(0).build());
                    seedSupportState(admin.getId());
                });

        game.setStatus(GameStatus.RUNNING);
        GameEntity saved = gameRepository.save(game);
        log.info("Game {} started with {} players", gameId, players.size());
        return saved;
    }

    @Transactional
    public GameEntity pauseGame(UUID gameId, UUID requestingPlayerId) {
        return transition(gameId, requestingPlayerId, GameStatus.RUNNING, GameStatus.PAUSED);
    }

    @Transactional
    public GameEntity resumeGame(UUID gameId, UUID requestingPlayerId) {
        return transition(gameId, requestingPlayerId, GameStatus.PAUSED, GameStatus.RUNNING);
    }

    @Transactional(readOnly = true)
    public GameEntity getGame(UUID gameId) {
        return requireGame(gameId);
    }

    @Transactional(readOnly = true)
    public List<PlayerEntity> getPlayers(UUID gameId) {
        return playerRepository.findByGameId(gameId);
    }

    @Transactional(readOnly = true)
    public PlayerEntity getPlayer(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
    }

    @Transactional(readOnly = true)
    public PlayerEconomyEntity getEconomy(UUID playerId) {
        return economyRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player economy not found: " + playerId));
    }

    @Transactional(readOnly = true)
    public PlayerMilitaryEntity getMilitary(UUID playerId) {
        return militaryRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player military not found: " + playerId));
    }

    @Transactional(readOnly = true)
    public PlayerEspionageEntity getEspionage(UUID playerId) {
        return espionageRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player espionage not found: " + playerId));
    }

    @Transactional(readOnly = true)
    public PlayerSpecialWeaponsEntity getSpecialWeapons(UUID playerId) {
        return specialWeaponsRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player special weapons not found: " + playerId));
    }

    @Transactional(readOnly = true)
    public PlayerProtectionEntity getProtection(UUID playerId) {
        return protectionRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player protection not found: " + playerId));
    }

    @Transactional(readOnly = true)
    public List<PlayerResearchEntity> getResearch(UUID playerId) {
        return researchRepository.findByPlayerId(playerId);
    }

    @Transactional(readOnly = true)
    public List<PlayerRelationEntity> getRelations(UUID playerId) {
        return relationRepository.findByPlayerLowOrPlayerHigh(playerId, playerId);
    }

    /** A viewer sees rivals' full state only with the Satellite Network research and a live satellite. */
    @Transactional(readOnly = true)
    public boolean hasSatelliteIntel(UUID viewerId) {
        return researchRepository.existsByPlayerIdAndResearchType(
                        viewerId, com.xmas.game.actions.domain.ActionType.RESEARCH_SATELLITE_NETWORK)
                && specialWeaponsRepository.findById(viewerId)
                        .map(s -> s.getEspionageSatellites() > 0)
                        .orElse(false);
    }

    private GameEntity transition(UUID gameId, UUID requestingPlayerId, GameStatus from, GameStatus to) {
        GameEntity game = requireGame(gameId);
        requireAdmin(gameId, requestingPlayerId);
        if (game.getStatus() != from) {
            throw new BadRequestException("Game is not " + from);
        }
        game.setStatus(to);
        return gameRepository.save(game);
    }

    private void seedSupportState(UUID playerId) {
        militaryRepository.save(PlayerMilitaryEntity.builder()
                .playerId(playerId).infantry(0).tanks(0).missileBatteries(0).airDefenseBatteries(0)
                .stealthBombers(0).specialOps(0).build());
        espionageRepository.save(PlayerEspionageEntity.builder()
                .playerId(playerId).spies(0).counterIntel(0).build());
        specialWeaponsRepository.save(PlayerSpecialWeaponsEntity.builder()
                .playerId(playerId).nukes(0).espionageSatellites(0).build());
        protectionRepository.save(PlayerProtectionEntity.builder()
                .playerId(playerId).doublegangers(0).bunkers(0).bodyguards(0).ninjaBodyguards(0).build());
    }

    private GameEntity requireGame(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Game not found: " + gameId));
    }

    private void requireAdmin(UUID gameId, UUID requestingPlayerId) {
        PlayerEntity player = playerRepository.findById(requestingPlayerId)
                .orElseThrow(() -> new NotFoundException("Player not found: " + requestingPlayerId));
        if (!player.getGameId().equals(gameId) || !player.isAdmin()) {
            throw new BadRequestException("Only the game admin can perform this operation");
        }
    }
}
