package com.omkaar.bank.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.omkaar.bank.model.LoanStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_requests")
public class LoanRequestEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    /* ---------- Constructors ---------- */

    protected LoanRequestEntity() {
        // JPA only
    }

    public LoanRequestEntity(UUID id,
            UUID accountId,
            BigDecimal amount,
            LoanStatus status,
            Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    /* ---------- Getters ---------- */

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    /* ---------- State transitions ---------- */

    public void markApproved() {
        this.status = LoanStatus.APPROVED;
        this.processedAt = Instant.now();
    }

    public void markRejected() {
        this.status = LoanStatus.REJECTED;
        this.processedAt = Instant.now();
    }
}
