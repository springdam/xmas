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

    /** The acting player (in the current game) for this token, or throws 401/404. */
    UUID requirePlayer(String sessionToken);
}
