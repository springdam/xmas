package com.xmas.game.engine.palace;

import com.xmas.game.combat.random.RandomSource;
import com.xmas.game.common.error.BadRequestException;
import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.engine.config.PalaceProperties;
import com.xmas.game.engine.domain.PalacePart;
import com.xmas.game.engine.domain.PalaceStyle;
import com.xmas.game.engine.entity.GameEntity;
import com.xmas.game.engine.entity.PlayerEntity;
import com.xmas.game.engine.entity.PlayerPalaceEntity;
import com.xmas.game.engine.repository.GameRepository;
import com.xmas.game.engine.repository.PlayerPalaceRepository;
import com.xmas.game.engine.repository.PlayerRepository;
import com.xmas.game.newsfeed.domain.NewsType;
import com.xmas.game.newsfeed.domain.NewsVisibility;
import com.xmas.game.newsfeed.service.NewsService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The palace: a trophy with no effect on play. Nations earn upgrade tokens at milestones (or pay
 * cash), choose which part to raise a tier, and lose tiers when rivals beat, nuke, sabotage or
 * vandalize them. Upgrades and damage are public news.
 */
@Service
@RequiredArgsConstructor
public class PalaceService {

    private final PlayerPalaceRepository palaceRepository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final PalaceProperties properties;
    private final NewsService newsService;
    private final RandomSource random;

    /** The player's palace, created bare on first use. */
    @Transactional
    public PlayerPalaceEntity palace(UUID playerId) {
        return palaceRepository.findById(playerId)
                .orElseGet(() -> palaceRepository.save(PlayerPalaceEntity.builder().playerId(playerId).build()));
    }

    /** Picks the palace's look; only once per game. */
    @Transactional
    public PlayerPalaceEntity chooseStyle(UUID playerId, PalaceStyle style) {
        PlayerEntity player = player(playerId);
        if (player.isAdmin()) {
            throw new BadRequestException("The admin has no palace");
        }
        PlayerPalaceEntity palace = palace(playerId);
        if (palace.getStyle() != null) {
            throw new BadRequestException("Your palace style is already chosen");
        }
        palace.setStyle(style);
        return palaceRepository.save(palace);
    }

    /** Palaces whose owner hasn't chosen yet are castles. */
    private static PalaceStyle style(PlayerPalaceEntity palace) {
        return palace.getStyle() == null ? PalaceStyle.CASTLE : palace.getStyle();
    }

    /** Grants one upgrade token; only the player hears about it. */
    @Transactional
    public void award(UUID playerId, String reason) {
        PlayerEntity player = player(playerId);
        if (player.isAdmin() || player.isEliminated()) {
            return;
        }
        PlayerPalaceEntity palace = palace(playerId);
        palace.setTokens(palace.getTokens() + 1);
        palaceRepository.save(palace);
        newsService.record(player.getGameId(), NewsType.DIPLOMACY, NewsVisibility.ACTOR_ONLY, playerId, null,
                "Your people honour you (%s): a palace improvement is yours to choose.".formatted(reason),
                gameTime(player));
    }

    /** Raises one part a tier, paid with a token or with cash. */
    @Transactional
    public PlayerPalaceEntity upgrade(UUID playerId, PalacePart part, boolean payWithCash) {
        PlayerEntity player = player(playerId);
        if (player.isAdmin() || player.isEliminated()) {
            throw new BadRequestException("Only a ruling nation can improve its palace");
        }
        PlayerPalaceEntity palace = palace(playerId);
        int next = palace.tier(part) + 1;
        if (next > properties.getMaxTier()) {
            throw new BadRequestException(capitalize(part.label()) + " is already as grand as it gets");
        }
        if (payWithCash) {
            long cost = tierCost(next);
            if (player.getCash() < cost) {
                throw new BadRequestException("Insufficient funds: %s costs $%d".formatted(
                        part.described(style(palace).tierName(next)), cost));
            }
            player.setCash(player.getCash() - cost);
            playerRepository.save(player);
        } else {
            if (palace.getTokens() <= 0) {
                throw new BadRequestException("No palace improvement earned yet: pay with cash instead");
            }
            palace.setTokens(palace.getTokens() - 1);
        }
        palace.setTier(part, next);
        palaceRepository.save(palace);
        newsService.record(player.getGameId(), NewsType.DIPLOMACY, NewsVisibility.PUBLIC, playerId, null,
                "%s's palace now boasts %s.".formatted(player.getNation(),
                        part.described(style(palace).tierName(next))),
                gameTime(player));
        return palace;
    }

    /**
     * Knocks {@code steps} tiers off random parts that still have any, and announces it.
     * {@code cause} completes "<cause> <NATION>'s palace", e.g. "CHINA's troops looted".
     */
    @Transactional
    public List<PalacePart> damage(UUID playerId, int steps, String cause) {
        PlayerPalaceEntity palace = palace(playerId);
        List<PalacePart> hit = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            List<PalacePart> standing = Arrays.stream(PalacePart.values()).filter(p -> palace.tier(p) > 0).toList();
            if (standing.isEmpty()) {
                break;
            }
            PalacePart part = standing.get(Math.min(standing.size() - 1, (int) (random.nextDouble() * standing.size())));
            palace.setTier(part, palace.tier(part) - 1);
            hit.add(part);
        }
        if (hit.isEmpty()) {
            return hit;
        }
        palaceRepository.save(palace);
        PlayerEntity player = player(playerId);
        newsService.record(player.getGameId(), NewsType.ATTACK, NewsVisibility.PUBLIC, null, playerId,
                "%s %s's palace, damaging %s.".formatted(cause, player.getNation(),
                        String.join(" and ", hit.stream().distinct().map(PalacePart::label).toList())),
                gameTime(player));
        return hit;
    }

    /** A lost battle costs a tier with chance = share of the defender's forces lost. */
    @Transactional
    public void onBattleLost(UUID defenderId, double lossFraction, Object attackerNation) {
        if (random.nextDouble() < lossFraction) {
            damage(defenderId, 1, "%s's troops looted".formatted(attackerNation));
        }
    }

    @Transactional
    public void onNuked(UUID targetId, int landedWarheads, Object attackerNation) {
        damage(targetId, landedWarheads * properties.getDamagePerLandedWarhead(),
                "%s's nuclear strike gutted".formatted(attackerNation));
    }

    /** Covert damage: the public news doesn't name who did it. */
    @Transactional
    public void onSabotaged(UUID targetId) {
        damage(targetId, properties.getSabotageDamage(), "Saboteurs wrecked");
    }

    @Transactional
    public void onVandalized(UUID targetId) {
        damage(targetId, properties.getVandalismDamage(), "Vandals defaced");
    }

    /** Palace score (0..24) without creating a palace that doesn't exist yet. */
    @Transactional(readOnly = true)
    public int score(UUID playerId) {
        return palaceRepository.findById(playerId).map(PlayerPalaceEntity::score).orElse(0);
    }

    /** One attack won; every few earn a token. */
    @Transactional
    public void recordAttackVictory(UUID attackerId) {
        PlayerPalaceEntity palace = palace(attackerId);
        palace.setAttackVictories(palace.getAttackVictories() + 1);
        palaceRepository.save(palace);
        if (palace.getAttackVictories() % properties.getVictoriesPerToken() == 0) {
            award(attackerId, "%d victories in battle".formatted(palace.getAttackVictories()));
        }
    }

    /** Per-tick milestones: staying in power, and staying popular. */
    @Transactional
    public void onTick(GameEntity game, PlayerEntity player) {
        long tick = game.getCurrentTick();
        if (tick > 0 && tick % properties.getTenureTicks() == 0) {
            award(player.getId(), "another stretch of your reign");
        }
        PlayerPalaceEntity palace = palace(player.getId());
        if (player.getPopularity() < properties.getPopularityThreshold()) {
            if (palace.getPopularSinceTick() != null) {
                palace.setPopularSinceTick(null);
                palaceRepository.save(palace);
            }
            return;
        }
        if (palace.getPopularSinceTick() == null) {
            palace.setPopularSinceTick(tick);
            palaceRepository.save(palace);
        } else if (tick - palace.getPopularSinceTick() >= properties.getPopularStreakTicks()) {
            palace.setPopularSinceTick(tick);
            palaceRepository.save(palace);
            award(player.getId(), "a long spell of popularity");
        }
    }

    /** Whether a war that started (or last changed) at {@code since} lasted long enough to earn a token on peace. */
    public boolean longWar(Instant since) {
        return since != null && since.isBefore(Instant.now().minusSeconds(properties.getMinWarMinutes() * 60));
    }

    public long tierCost(int tier) {
        List<Long> costs = properties.getTierCosts();
        return costs.get(Math.max(0, Math.min(tier, costs.size()) - 1));
    }

    public List<Long> tierCosts() {
        return properties.getTierCosts();
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private Instant gameTime(PlayerEntity player) {
        return gameRepository.findById(player.getGameId())
                .map(GameEntity::getGameTime)
                .orElse(Instant.now());
    }

    private PlayerEntity player(UUID playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
    }
}
