package com.omkaar.bank.service;

import java.math.BigDecimal;

import com.omkaar.bank.model.Account;
import com.omkaar.bank.model.AccountType;
import com.omkaar.bank.model.CurrentAccount;
import com.omkaar.bank.model.SavingsAccount;

public class AccountFactory {

    private AccountFactory() {
        // prevented instantiation
    }

    public static Account createAccount(AccountType type, BigDecimal initialBalance) {

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }

        return switch (type) {
            case SAVINGS -> new SavingsAccount(initialBalance);
            case CURRENT -> new CurrentAccount(initialBalance);
        };
    }
}
