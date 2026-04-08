package com.omkaar.bank.mapper;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.model.Account;
import com.omkaar.bank.model.CurrentAccount;
import com.omkaar.bank.model.SavingsAccount;

public final class AccountMapper {

    private AccountMapper() {
        // utility class
    }

    // Domain → Entity
    public static AccountEntity toEntity(Account domain) {
        Objects.requireNonNull(domain, "Account must not be null");

        return new AccountEntity(
                UUID.fromString(domain.getAccountId()),
                domain.getType(),
                domain.getBalance(),
                domain.isFrozen(),
                Instant.now() // OK ONLY on creation
        );
    }

    // Entity → Domain
    public static Account toDomain(AccountEntity entity) {

        return switch (entity.getType()) {

            case SAVINGS -> new SavingsAccount(
                    entity.getId().toString(),
                    entity.getBalance(),
                    entity.isFrozen());

            case CURRENT -> new CurrentAccount(
                    entity.getId().toString(),
                    entity.getBalance(),
                    entity.isFrozen());
        };
    }

}
