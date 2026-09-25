package com.xmas.game.account.mapper;

import com.xmas.game.account.api.model.AccountResponse;
import com.xmas.game.account.entity.AccountEntity;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(AccountEntity entity) {
        return new AccountResponse()
                .id(entity.getId())
                .username(entity.getUsername());
    }
}
