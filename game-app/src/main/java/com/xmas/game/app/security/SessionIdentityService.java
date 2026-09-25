package com.xmas.game.app.security;

import com.xmas.game.account.session.SessionStore;
import com.xmas.game.common.error.NotFoundException;
import com.xmas.game.common.error.UnauthorizedException;
import com.xmas.game.common.security.SessionIdentity;
import com.xmas.game.engine.repository.PlayerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionIdentityService implements SessionIdentity {

    private final SessionStore sessionStore;
    private final PlayerRepository playerRepository;

    @Override
    public UUID requireAccountId(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new UnauthorizedException("Missing session token");
        }
        return sessionStore.resolve(sessionToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));
    }

    @Override
    public UUID requirePlayer(String sessionToken) {
        UUID accountId = requireAccountId(sessionToken);
        return playerRepository.findFirstByAccountIdOrderByJoinedAtDesc(accountId)
                .orElseThrow(() -> new NotFoundException("No player for this account; join a game first"))
                .getId();
    }
}
