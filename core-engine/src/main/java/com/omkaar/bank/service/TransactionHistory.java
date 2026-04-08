package com.omkaar.bank.service;

import java.util.List;

import com.omkaar.bank.model.Transaction;

public interface TransactionHistory {

    void record(Transaction transaction);

    List<Transaction> getAll();

    List<Transaction> getByAccountId(String accountId);
}
