package com.xmas.game.account.session;

import com.xmas.game.account.config.AccountProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!develop")
@RequiredArgsConstructor
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redisTemplate;
    private final AccountProperties properties;

    @Override
    public String create(UUID accountId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                accountId.toString(),
                Duration.ofMinutes(properties.getSessionTtlMinutes()));
        return token;
    }

    @Override
    public Optional<UUID> resolve(String sessionToken) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + sessionToken);
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    @Override
    public void invalidate(String sessionToken) {
        redisTemplate.delete(KEY_PREFIX + sessionToken);
    }
}
