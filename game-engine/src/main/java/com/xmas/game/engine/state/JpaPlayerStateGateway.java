package com.xmas.game.engine.state;

import com.xmas.game.actions.domain.ActionType;
import com.xmas.game.actions.state.PlayerStateGateway;
import com.xmas.game.engine.domain.PendingActionStatus;
import com.xmas.game.engine.economy.LandService;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEconomyEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerEspionageEntity;
import com.xmas.game.engine.entity.PlayerMilitaryEntity;
import com.xmas.game.engine.entity.PlayerProtectionEntity;
import com.xmas.game.engine.entity.PlayerSpecialWeaponsEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PendingActionRepository;
import com.xmas.game.engine.repository.PlayerEconomyRepository;
import com.xmas.game.engine.repository.PlayerEspionageRepository;
import com.xmas.game.engine.repository.PlayerMilitaryRepository;
import com.xmas.game.engine.repository.PlayerProtectionRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.engine.repository.PlayerSpecialWeaponsRepository;
import com.xmas.game.common.error.BadRequestException;
import com.xmas.game.common.error.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaPlayerStateGateway implements PlayerStateGateway {

    private final PlayerRepository playerRepository;
    private final PlayerEconomyRepository economyRepository;
    private final PlayerMilitaryRepository militaryRepository;
    private final PlayerEspionageRepository espionageRepository;
    private final PlayerSpecialWeaponsRepository specialWeaponsRepository;
    private final PlayerProtectionRepository protectionRepository;
    private final GameRepository gameRepository;
    private final LandService landService;
    private final PendingActionRepository pendingActionRepository;

    @Override
    @Transactional(readOnly = true)
    public void assertCanAct(UUID playerId) {
        PlayerEntity player = player(playerId);
        if (player.isAdmin()) {
            throw new BadRequestException("The admin does not control a nation and cannot perform actions");
        }
        if (player.isEliminated()) {
            throw new BadRequestException("This nation's government has collapsed and can no longer act");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void assertNotFrozen(UUID playerId, boolean militaryAction) {
        if (militaryAction) {
            return;
        }
        PlayerEntity player = player(playerId);
        if (player.getFrozenUntilTick() <= 0) {
            return;
        }
        long currentTick = gameRepository.findById(player.getGameId())
                .map(GameEntity::getCurrentTick)
                .orElse(Long.MAX_VALUE);
        if (currentTick < player.getFrozenUntilTick()) {
            throw new BadRequestException(
                    "Your government is holding an election; only military actions are allowed until tick "
                            + player.getFrozenUntilTick());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getCash(UUID playerId) {
        return player(playerId).getCash();
    }

    @Override
    @Transactional
    public void charge(UUID playerId, long amount) {
        PlayerEntity player = player(playerId);
        player.setCash(player.getCash() - amount);
        playerRepository.save(player);
    }

    @Override
    @Transactional
    public void addAsset(UUID playerId, ActionType assetType, int quantity) {
        switch (assetType) {
            case BUILD_FARM, BUILD_FACTORY, BUILD_ROBOT_FACTORY, BUILD_BANK -> {
                PlayerEconomyEntity economy = economy(playerId);
                switch (assetType) {
                    case BUILD_FARM -> economy.setFarms(economy.getFarms() + quantity);
                    case BUILD_FACTORY -> economy.setFactories(economy.getFactories() + quantity);
                    case BUILD_ROBOT_FACTORY -> economy.setRobotFactories(economy.getRobotFactories() + quantity);
                    case BUILD_BANK -> economy.setBanks(economy.getBanks() + quantity);
                    default -> throw new BadRequestException(assetType + " is not a storable asset");
                }
                economyRepository.save(economy);
            }
            case RECRUIT_INFANTRY, RECRUIT_TANK, BUILD_MISSILE_BATTERY, BUILD_AIR_DEFENSE_BATTERY,
                    BUILD_STEALTH_BOMBER, RECRUIT_SPECIAL_OPS -> {
                PlayerMilitaryEntity military = military(playerId);
                switch (assetType) {
                    case RECRUIT_INFANTRY -> military.setInfantry(military.getInfantry() + quantity);
                    case RECRUIT_TANK -> military.setTanks(military.getTanks() + quantity);
                    case BUILD_MISSILE_BATTERY -> military.setMissileBatteries(military.getMissileBatteries() + quantity);
                    case BUILD_AIR_DEFENSE_BATTERY ->
                            military.setAirDefenseBatteries(military.getAirDefenseBatteries() + quantity);
                    case BUILD_STEALTH_BOMBER -> military.setStealthBombers(military.getStealthBombers() + quantity);
                    case RECRUIT_SPECIAL_OPS -> military.setSpecialOps(military.getSpecialOps() + quantity);
                    default -> throw new BadRequestException(assetType + " is not a storable asset");
                }
                militaryRepository.save(military);
            }
            case HIRE_SPIES, HIRE_COUNTER_INTEL -> {
                PlayerEspionageEntity espionage = espionage(playerId);
                switch (assetType) {
                    case HIRE_SPIES -> espionage.setSpies(espionage.getSpies() + quantity);
                    case HIRE_COUNTER_INTEL -> espionage.setCounterIntel(espionage.getCounterIntel() + quantity);
                    default -> throw new BadRequestException(assetType + " is not a storable asset");
                }
                espionageRepository.save(espionage);
            }
            case BUILD_NUKE, BUILD_ESPIONAGE_SATELLITE -> {
                PlayerSpecialWeaponsEntity special = special(playerId);
                switch (assetType) {
                    case BUILD_NUKE -> special.setNukes(special.getNukes() + quantity);
                    case BUILD_ESPIONAGE_SATELLITE ->
                            special.setEspionageSatellites(special.getEspionageSatellites() + quantity);
                    default -> throw new BadRequestException(assetType + " is not a storable asset");
                }
                specialWeaponsRepository.save(special);
            }
            case HIRE_DOUBLEGANGER, BUILD_SECRET_BUNKER, HIRE_BODYGUARDS, HIRE_NINJA_BODYGUARDS -> {
                PlayerProtectionEntity protection = protection(playerId);
                switch (assetType) {
                    case HIRE_DOUBLEGANGER -> protection.setDoublegangers(protection.getDoublegangers() + quantity);
                    case BUILD_SECRET_BUNKER -> protection.setBunkers(protection.getBunkers() + quantity);
                    case HIRE_BODYGUARDS -> protection.setBodyguards(protection.getBodyguards() + quantity);
                    case HIRE_NINJA_BODYGUARDS ->
                            protection.setNinjaBodyguards(protection.getNinjaBodyguards() + quantity);
                    default -> throw new BadRequestException(assetType + " is not a storable asset");
                }
                protectionRepository.save(protection);
            }
            default -> throw new BadRequestException(assetType + " is not a storable asset");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getFreeLand(UUID playerId) {
        return landService.free(playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingAgainst(UUID playerId, ActionType type, UUID targetPlayerId) {
        return pendingActionRepository.existsByActorPlayerIdAndActionTypeAndTargetPlayerIdAndStatus(
                playerId, type, targetPlayerId, PendingActionStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAssetCount(UUID playerId, ActionType assetType) {
        return switch (assetType) {
            case BUILD_FARM -> economy(playerId).getFarms();
            case BUILD_FACTORY -> economy(playerId).getFactories();
            case BUILD_ROBOT_FACTORY -> economy(playerId).getRobotFactories();
            case BUILD_BANK -> economy(playerId).getBanks();
            case RECRUIT_INFANTRY -> military(playerId).getInfantry();
            case RECRUIT_TANK -> military(playerId).getTanks();
            case BUILD_MISSILE_BATTERY -> military(playerId).getMissileBatteries();
            case BUILD_AIR_DEFENSE_BATTERY -> military(playerId).getAirDefenseBatteries();
            case BUILD_STEALTH_BOMBER -> military(playerId).getStealthBombers();
            case RECRUIT_SPECIAL_OPS -> military(playerId).getSpecialOps();
            case HIRE_SPIES -> espionage(playerId).getSpies();
            case HIRE_COUNTER_INTEL -> espionage(playerId).getCounterIntel();
            case BUILD_NUKE -> special(playerId).getNukes();
            case BUILD_ESPIONAGE_SATELLITE -> special(playerId).getEspionageSatellites();
            case HIRE_DOUBLEGANGER -> protection(playerId).getDoublegangers();
            case BUILD_SECRET_BUNKER -> protection(playerId).getBunkers();
            case HIRE_BODYGUARDS -> protection(playerId).getBodyguards();
            case HIRE_NINJA_BODYGUARDS -> protection(playerId).getNinjaBodyguards();
            default -> throw new BadRequestException(assetType + " is not a storable asset");
        };
    }

    private PlayerEntity player(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
    }

    private PlayerEconomyEntity economy(UUID playerId) {
        return economyRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player economy not found: " + playerId));
    }

    private PlayerMilitaryEntity military(UUID playerId) {
        return militaryRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player military not found: " + playerId));
    }

    private PlayerEspionageEntity espionage(UUID playerId) {
        return espionageRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player espionage not found: " + playerId));
    }

    private PlayerSpecialWeaponsEntity special(UUID playerId) {
        return specialWeaponsRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player special weapons not found: " + playerId));
    }

    private PlayerProtectionEntity protection(UUID playerId) {
        return protectionRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player protection not found: " + playerId));
    }
}
