package com.xmas.game.account.rest;

import com.xmas.game.account.api.AccountApi;
import com.xmas.game.account.api.model.AccountResponse;
import com.xmas.game.account.api.model.LoginRequest;
import com.xmas.game.account.api.model.LoginResponse;
import com.xmas.game.account.api.model.RegisterRequest;
import com.xmas.game.account.entity.AccountEntity;
import com.xmas.game.account.mapper.AccountMapper;
import com.xmas.game.account.service.AccountService;
import com.xmas.game.account.service.LoginResult;
import com.xmas.game.account.api.model.AvatarRequest;
import com.xmas.game.account.session.SessionStore;
import com.xmas.game.common.error.UnauthorizedException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final AccountService accountService;
    private final SessionStore sessionStore;

    @Override
    public ResponseEntity<AccountResponse> register(RegisterRequest registerRequest) {
        AccountEntity account = accountService.register(
                registerRequest.getUsername(), registerRequest.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountMapper.toResponse(account));
    }

    @Override
    public ResponseEntity<Void> setAvatar(String xSessionToken, AvatarRequest avatarRequest) {
        UUID accountId = sessionStore.resolve(xSessionToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));
        accountService.setAvatar(accountId, avatarRequest.getImage());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Resource> getAvatar(UUID accountId) {
        // Clients version the URL (?v=...), so a portrait can be cached for good.
        return accountService.avatar(accountId)
                .map(png -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                        .body((Resource) new ByteArrayResource(png)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        LoginResult result = accountService.login(
                loginRequest.getUsername(), loginRequest.getPassword());
        LoginResponse response = new LoginResponse()
                .accountId(result.getAccountId())
                .sessionToken(result.getSessionToken());
        return ResponseEntity.ok(response);
    }
}
