package com.omkaar.bank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class LoanRequest {

    private final String requestId;
    private final String accountId;
    private final BigDecimal amount;
    private final LocalDateTime requestedAt;

    private final Queue<LoanRequest> loanQueue = new LinkedList<>();

    public LoanRequest(String accountId, BigDecimal amount) {
        this.requestId = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.amount = amount;
        this.requestedAt = LocalDateTime.now();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void requestLoan(String accountId, BigDecimal amount) {
        loanQueue.add(new LoanRequest(accountId, amount));
    }

}
