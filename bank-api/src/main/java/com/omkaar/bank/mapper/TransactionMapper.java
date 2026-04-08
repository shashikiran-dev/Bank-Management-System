package com.omkaar.bank.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.omkaar.bank.entity.TransactionEntity;
import com.omkaar.bank.model.TransactionType;
import com.omkaar.bank.model.TransactionView;

public final class TransactionMapper {

    private TransactionMapper() {
        // utility class
    }

    public static TransactionView toView(TransactionEntity entity) {
        return new TransactionView(
                entity.getId(),
                entity.getType(),
                entity.getFromAccountId(),
                entity.getToAccountId(),
                entity.getAmount(),
                entity.getTimestamp());
    }

    public static TransactionEntity deposit(UUID toAccountId,
            BigDecimal amount) {
        return new TransactionEntity(
                UUID.randomUUID(),
                TransactionType.DEPOSIT,
                null,
                toAccountId,
                amount,
                Instant.now());
    }

    public static TransactionEntity withdraw(UUID fromAccountId,
            BigDecimal amount) {
        return new TransactionEntity(
                UUID.randomUUID(),
                TransactionType.WITHDRAWAL,
                fromAccountId,
                null,
                amount,
                Instant.now());
    }

    public static TransactionEntity transfer(UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount) {
        return new TransactionEntity(
                UUID.randomUUID(),
                TransactionType.TRANSFER,
                fromAccountId,
                toAccountId,
                amount,
                Instant.now());
    }
}
