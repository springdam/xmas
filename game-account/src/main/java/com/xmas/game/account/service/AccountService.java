package com.xmas.game.account.service;

import com.xmas.game.account.entity.AccountEntity;
import com.xmas.game.account.repository.AccountRepository;
import com.xmas.game.account.session.SessionStore;
import com.xmas.game.common.error.ConflictException;
import com.xmas.game.common.error.UnauthorizedException;
import java.time.Instant;
import java.util.UUID;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import com.xmas.game.common.error.BadRequestException;
import com.xmas.game.common.error.NotFoundException;
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

    /** Largest accepted portrait, base64 characters (a 64x64 PNG is far below this). */
    private static final int MAX_AVATAR_CHARS = 24_000;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G'};

    /** Stores the player's pixel portrait (a base64 PNG made in the browser). */
    @Transactional
    public void setAvatar(UUID accountId, String base64Png) {
        if (base64Png == null || base64Png.isBlank() || base64Png.length() > MAX_AVATAR_CHARS) {
            throw new BadRequestException("The portrait must be a small PNG image");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Png);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("The portrait is not valid base64");
        }
        if (bytes.length < PNG_SIGNATURE.length || !Arrays.equals(Arrays.copyOf(bytes, 4), PNG_SIGNATURE)) {
            throw new BadRequestException("The portrait must be a PNG image");
        }
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        account.setAvatar(base64Png);
        account.setAvatarUpdatedAt(Instant.now());
        accountRepository.save(account);
    }

    /** The portrait's PNG bytes, if the player has one. */
    @Transactional(readOnly = true)
    public Optional<byte[]> avatar(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(AccountEntity::getAvatar)
                .map(Base64.getDecoder()::decode);
    }
}
