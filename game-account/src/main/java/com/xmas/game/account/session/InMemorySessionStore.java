package com.xmas.game.account.session;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("develop")
public class InMemorySessionStore implements SessionStore {

    private final ConcurrentHashMap<String, UUID> sessions = new ConcurrentHashMap<>();

    @Override
    public String create(UUID accountId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, accountId);
        return token;
    }

    @Override
    public Optional<UUID> resolve(String sessionToken) {
        return Optional.ofNullable(sessions.get(sessionToken));
    }

    @Override
    public void invalidate(String sessionToken) {
        sessions.remove(sessionToken);
    }
}
