package com.omkaar.bank.model;

import java.math.BigDecimal;

public class SavingsAccount extends Account {

    // Used when creating a NEW account
    public SavingsAccount(BigDecimal initialBalance) {
        super(initialBalance);
    }

    // Used when LOADING from persistence
    public SavingsAccount(String accountId,
            BigDecimal balance,
            boolean frozen) {
        super(accountId, balance, frozen);
    }

    @Override
    public AccountType getType() {
        return AccountType.SAVINGS;
    }

    @Override
    public void applyMonthlyInterest() {
        // implement later
    }
}
