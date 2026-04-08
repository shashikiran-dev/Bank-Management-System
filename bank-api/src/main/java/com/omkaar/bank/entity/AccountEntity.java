package com.omkaar.bank.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.omkaar.bank.model.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private boolean frozen;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /* ---------- Constructors ---------- */

    protected AccountEntity() {
        // JPA only
    }

    public AccountEntity(UUID id,
            AccountType type,
            BigDecimal balance,
            boolean frozen,
            Instant createdAt) {
        this.id = id;
        this.type = type;
        this.balance = balance;
        this.frozen = frozen;
        this.createdAt = createdAt;
    }

    /* ---------- Getters ---------- */

    public UUID getId() {
        return id;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /* ---------- Setters (restricted) ---------- */

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
}
