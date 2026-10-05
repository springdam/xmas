package com.xmas.game.common.security;

import java.util.UUID;

/**
 * Resolves the caller's identity from their session token. Implemented in the
 * application module over the account session store and the player repository,
 * so controllers never trust a client-supplied account or player id.
 */
public interface SessionIdentity {

    /** The authenticated account for this token, or throws 401 if the token is missing/invalid. */
    UUID requireAccountId(String sessionToken);

    /**
     * The caller's player in the given game, or throws 401 (bad token) / 403 (not in that game).
     * Always game-scoped: an account can be in several games (an open lobby and a running game).
     */
    UUID requirePlayer(String sessionToken, UUID gameId);
}
