package com.omkaar.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class Transaction {

    private final String transactionId;
    private final TransactionType type;
    private final String fromAccountId;
    private final String toAccountId;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;

    public Transaction(
            TransactionType type,
            String fromAccountId,
            String toAccountId,
            BigDecimal amount) {
        this.transactionId = UUID.randomUUID().toString();
        this.type = type;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
