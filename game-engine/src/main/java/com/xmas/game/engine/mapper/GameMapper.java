package com.xmas.game.engine.mapper;

import com.xmas.game.engine.api.model.EconomyResponse;
import com.xmas.game.engine.api.model.GameListing;
import com.xmas.game.engine.api.model.GamePlayerSummary;
import com.xmas.game.engine.api.model.GameResponse;
import com.xmas.game.engine.api.model.GameStatus;
import com.xmas.game.engine.api.model.EspionageResponse;
import com.xmas.game.engine.api.model.MilitaryResponse;
import com.xmas.game.engine.api.model.PalaceResponse;
import com.xmas.game.engine.api.model.Nation;
import com.xmas.game.engine.api.model.PlayerResponse;
import com.xmas.game.engine.api.model.PlayerStateResponse;
import com.xmas.game.engine.api.model.ProtectionResponse;
import com.xmas.game.engine.api.model.RelationResponse;
import com.xmas.game.engine.api.model.RelationStatus;
import com.xmas.game.engine.api.model.SpecialWeaponsResponse;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerEspionageEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.entity.PlayerPalaceEntity;
import com.xmas.game.engine.entity.PlayerProtectionEntity;
import com.xmas.game.engine.entity.PlayerRelationEntity;
import com.xmas.game.engine.entity.PlayerResearchEntity;
import com.xmas.game.engine.entity.PlayerSpecialWeaponsEntity;
import com.xmas.game.engine.service.GameService;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class GameMapper {

    private GameMapper() {
    }

    public static GameResponse toGameResponse(GameEntity game) {
        return new GameResponse()
                .id(game.getId())
                .status(GameStatus.fromValue(game.getStatus().name()))
                .currentTick(game.getCurrentTick())
                .gameTime(game.getGameTime().atOffset(ZoneOffset.UTC))
                .tickDurationMillis(game.getTickDurationMillis())
                .winnerPlayerId(game.getWinnerPlayerId());
    }

    public static GameListing toGameListing(GameService.Listing listing, String adminUsername) {
        return new GameListing()
                .id(listing.game().getId())
                .status(GameStatus.fromValue(listing.game().getStatus().name()))
                .createdAt(listing.game().getCreatedAt().atOffset(ZoneOffset.UTC))
                .adminUsername(adminUsername)
                .nations((int) listing.players().stream().filter(p -> !p.isAdmin()).count())
                .maxNations(com.xmas.game.common.domain.Nation.values().length)
                .joined(listing.joined());
    }

    public static PlayerResponse toPlayerResponse(PlayerEntity player) {
        return new PlayerResponse()
                .id(player.getId())
                .gameId(player.getGameId())
                .accountId(player.getAccountId())
                .nation(player.getNation() == null ? null : Nation.fromValue(player.getNation().name()))
                .cash(player.getCash())
                .popularity(player.getPopularity())
                .admin(player.isAdmin())
                .eliminated(player.isEliminated())
                .popularityBoostCount(player.getPopularityBoostCount())
                .frozenUntilTick(player.getFrozenUntilTick());
    }

    public static EconomyResponse toEconomyResponse(PlayerEconomyEntity economy, int landCapacity, int landInUse,
                                                    int industryCapacity) {
        return new EconomyResponse()
                .farms(economy.getFarms())
                .factories(economy.getFactories())
                .robotFactories(economy.getRobotFactories())
                .banks(economy.getBanks())
                .landCapacity(landCapacity)
                .landInUse(landInUse)
                .industryCapacity(industryCapacity);
    }

    public static GamePlayerSummary toGamePlayerSummary(PlayerEntity player, String username) {
        return new GamePlayerSummary()
                .id(player.getId())
                .username(username)
                .nation(player.getNation() == null ? null : Nation.fromValue(player.getNation().name()))
                .popularity(player.getPopularity())
                .eliminated(player.isEliminated())
                .admin(player.isAdmin());
    }

    public static MilitaryResponse toMilitaryResponse(PlayerMilitaryEntity military, long upkeepPerTick) {
        return new MilitaryResponse()
                .infantry(military.getInfantry())
                .tanks(military.getTanks())
                .missileBatteries(military.getMissileBatteries())
                .airDefenseBatteries(military.getAirDefenseBatteries())
                .stealthBombers(military.getStealthBombers())
                .specialOps(military.getSpecialOps())
                .upkeepPerTick(upkeepPerTick);
    }

    public static EspionageResponse toEspionageResponse(PlayerEspionageEntity espionage) {
        return new EspionageResponse()
                .spies(espionage.getSpies())
                .counterIntel(espionage.getCounterIntel());
    }

    public static SpecialWeaponsResponse toSpecialWeaponsResponse(PlayerSpecialWeaponsEntity special) {
        return new SpecialWeaponsResponse()
                .nukes(special.getNukes())
                .espionageSatellites(special.getEspionageSatellites());
    }

    public static ProtectionResponse toProtectionResponse(PlayerProtectionEntity protection) {
        return new ProtectionResponse()
                .doublegangers(protection.getDoublegangers())
                .bunkers(protection.getBunkers())
                .bodyguards(protection.getBodyguards())
                .ninjaBodyguards(protection.getNinjaBodyguards());
    }

    public static RelationResponse toRelationResponse(UUID playerId, PlayerRelationEntity relation) {
        UUID other = relation.getPlayerLow().equals(playerId) ? relation.getPlayerHigh() : relation.getPlayerLow();
        return new RelationResponse()
                .otherPlayerId(other)
                .status(RelationStatus.fromValue(relation.getStatus().name()));
    }

    public static PalaceResponse toPalaceResponse(PlayerPalaceEntity palace, long value, boolean own,
                                                  List<Long> tierCosts) {
        return new PalaceResponse()
                .walls(palace.getWalls())
                .floor(palace.getFloor())
                .throne(palace.getThrone())
                .pillars(palace.getPillars())
                .windows(palace.getWindows())
                .treasures(palace.getTreasures())
                .score(palace.score())
                .value(value)
                .style(palace.getStyle() == null ? null
                        : com.xmas.game.engine.api.model.PalaceStyle.fromValue(palace.getStyle().name()))
                .tokens(own ? palace.getTokens() : null)
                .tierCosts(tierCosts);
    }

    public static PlayerStateResponse toLimitedPlayerState(PlayerEntity player, List<PlayerRelationEntity> relations) {
        return new PlayerStateResponse()
                .player(toPlayerResponse(player))
                .relations(relations.stream().map(r -> toRelationResponse(player.getId(), r)).toList());
    }

    public static PlayerStateResponse toPlayerState(PlayerEntity player, PlayerEconomyEntity economy,
                                                    int landCapacity, int landInUse, int industryCapacity,
                                                    PlayerMilitaryEntity military, long upkeepPerTick,
                                                    PlayerEspionageEntity espionage,
                                                    PlayerSpecialWeaponsEntity specialWeapons,
                                                    PlayerProtectionEntity protection,
                                                    List<PlayerResearchEntity> research,
                                                    List<PlayerRelationEntity> relations) {
        return new PlayerStateResponse()
                .player(toPlayerResponse(player))
                .economy(toEconomyResponse(economy, landCapacity, landInUse, industryCapacity))
                .military(toMilitaryResponse(military, upkeepPerTick))
                .espionage(toEspionageResponse(espionage))
                .specialWeapons(toSpecialWeaponsResponse(specialWeapons))
                .protection(toProtectionResponse(protection))
                .research(research.stream().map(r -> r.getResearchType().name()).toList())
                .researchLevels(research.stream().collect(java.util.stream.Collectors.toMap(
                        r -> r.getResearchType().name(), PlayerResearchEntity::getLevel)))
                .relations(relations.stream().map(r -> toRelationResponse(player.getId(), r)).toList());
    }
}
