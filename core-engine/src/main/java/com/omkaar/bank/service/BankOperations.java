package com.omkaar.bank.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.omkaar.bank.model.Account;
import com.omkaar.bank.model.TransactionView;

public interface BankOperations {

    void registerAccount(Account account);

    void deposit(UUID accountId, BigDecimal amount);

    void withdraw(UUID accountId, BigDecimal amount);

    void transfer(UUID fromId, UUID toId, BigDecimal amount);

    void requestLoan(UUID accountId, BigDecimal amount);

    void processNextLoan();

    List<TransactionView> getTransactionHistory(UUID accountId);

    void undoLastTransaction();

}
