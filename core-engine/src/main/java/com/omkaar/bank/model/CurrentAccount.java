package com.omkaar.bank.model;

import java.math.BigDecimal;

public class CurrentAccount extends Account {

    // New account creation
    public CurrentAccount(BigDecimal initialBalance) {
        super(initialBalance);
    }

    // Rehydration from DB
    public CurrentAccount(String accountId,
            BigDecimal balance,
            boolean frozen) {
        super(accountId, balance, frozen);
    }

    @Override
    public AccountType getType() {
        return AccountType.CURRENT;
    }

    @Override
    public void applyMonthlyInterest() {
        // Typically none for current accounts
    }
}
