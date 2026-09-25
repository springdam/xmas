package com.xmas.game.account.service;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResult {

    private final UUID accountId;
    private final String sessionToken;
}
