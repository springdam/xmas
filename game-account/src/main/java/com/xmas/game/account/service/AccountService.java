package com.xmas.game.account.service;

import com.xmas.game.account.entity.AccountEntity;
import com.xmas.game.account.repository.AccountRepository;
import com.xmas.game.account.session.SessionStore;
import com.xmas.game.common.error.ConflictException;
import com.xmas.game.common.error.UnauthorizedException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionStore sessionStore;

    @Transactional
    public AccountEntity register(String username, String rawPassword) {
        if (accountRepository.existsByUsername(username)) {
            throw new ConflictException("Username already taken");
        }
        AccountEntity account = AccountEntity.builder()
                .id(UUID.randomUUID())
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .createdAt(Instant.now())
                .build();
        AccountEntity saved = accountRepository.save(account);
        log.info("Registered account {} ({})", saved.getUsername(), saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public LoginResult login(String username, String rawPassword) {
        AccountEntity account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = sessionStore.create(account.getId());
        return new LoginResult(account.getId(), token);
    }
}
