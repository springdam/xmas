package com.xmas.game.account.repository;

import com.xmas.game.account.entity.AccountEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
