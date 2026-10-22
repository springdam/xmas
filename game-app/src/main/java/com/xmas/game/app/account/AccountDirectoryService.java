package com.xmas.game.app.account;

import com.xmas.game.account.entity.AccountEntity;
import com.xmas.game.account.repository.AccountRepository;
import com.xmas.game.common.account.AccountDirectory;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccountDirectoryService implements AccountDirectory {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> usernames(Collection<UUID> accountIds) {
        return accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(AccountEntity::getId, AccountEntity::getUsername));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Instant> avatarVersions(Collection<UUID> accountIds) {
        return accountRepository.findAllById(accountIds).stream()
                .filter(account -> account.getAvatar() != null && account.getAvatarUpdatedAt() != null)
                .collect(Collectors.toMap(AccountEntity::getId, AccountEntity::getAvatarUpdatedAt));
    }
}
