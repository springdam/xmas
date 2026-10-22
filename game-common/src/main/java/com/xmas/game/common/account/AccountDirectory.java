package com.xmas.game.common.account;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Looks up public account details (the username) for other modules. Implemented in the
 * application module over the account repository, so business modules need not depend
 * on game-account.
 */
public interface AccountDirectory {

    /** Usernames keyed by account id; ids with no account are absent from the map. */
    Map<UUID, String> usernames(Collection<UUID> accountIds);

    /** When each account's portrait last changed; accounts without a portrait are absent. */
    Map<UUID, Instant> avatarVersions(Collection<UUID> accountIds);
}
