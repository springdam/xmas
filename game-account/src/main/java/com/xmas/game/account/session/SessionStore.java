package com.xmas.game.account.session;

import java.util.Optional;
import java.util.UUID;

public interface SessionStore {

    String create(UUID accountId);

    Optional<UUID> resolve(String sessionToken);

    void invalidate(String sessionToken);
}
