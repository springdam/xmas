package com.xmas.game.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.xmas.game.account.entity.AccountEntity;
import com.xmas.game.account.repository.AccountRepository;
import com.xmas.game.account.session.SessionStore;
import com.xmas.game.common.error.ConflictException;
import com.xmas.game.common.error.UnauthorizedException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private SessionStore sessionStore;
    private PasswordEncoder passwordEncoder;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountRepository.class);
        sessionStore = Mockito.mock(SessionStore.class);
        passwordEncoder = new BCryptPasswordEncoder();
        accountService = new AccountService(accountRepository, passwordEncoder, sessionStore);
    }

    @Test
    void register_persistsHashedPassword() {
        when(accountRepository.existsByUsername("alice")).thenReturn(false);
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountEntity saved = accountService.register("alice", "secret1");

        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret1");
        assertThat(passwordEncoder.matches("secret1", saved.getPasswordHash())).isTrue();
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(accountRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> accountService.register("alice", "secret1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_returnsSessionTokenOnValidCredentials() {
        AccountEntity account = AccountEntity.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .passwordHash(passwordEncoder.encode("secret1"))
                .build();
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));
        when(sessionStore.create(account.getId())).thenReturn("token-123");

        LoginResult result = accountService.login("alice", "secret1");

        assertThat(result.getAccountId()).isEqualTo(account.getId());
        assertThat(result.getSessionToken()).isEqualTo("token-123");
    }

    @Test
    void login_rejectsWrongPassword() {
        AccountEntity account = AccountEntity.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .passwordHash(passwordEncoder.encode("secret1"))
                .build();
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.login("alice", "wrong"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
