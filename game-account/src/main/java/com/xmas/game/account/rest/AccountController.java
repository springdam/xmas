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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final AccountService accountService;

    @Override
    public ResponseEntity<AccountResponse> register(RegisterRequest registerRequest) {
        AccountEntity account = accountService.register(
                registerRequest.getUsername(), registerRequest.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountMapper.toResponse(account));
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
