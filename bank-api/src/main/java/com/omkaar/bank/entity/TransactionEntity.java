package com.omkaar.bank.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.omkaar.bank.model.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "from_account_id")
    private UUID fromAccountId;

    @Column(name = "to_account_id")
    private UUID toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    /* ---------- Constructors ---------- */

    protected TransactionEntity() {
        // JPA only
    }

    public TransactionEntity(UUID id,
            TransactionType type,
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            Instant timestamp) {
        this.id = id;
        this.type = type;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    /* ---------- Getters ---------- */

    public UUID getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
