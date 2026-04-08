package com.omkaar.bank.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.omkaar.bank.model.Transaction;

public class InMemoryTransactionHistory implements TransactionHistory {

    private final List<Transaction> transactions = new ArrayList<>();

    @Override
    public void record(Transaction transaction) {
        transactions.add(transaction);
    }

    @Override
    public List<Transaction> getAll() {
        return List.copyOf(transactions);
    }

    @Override
    public List<Transaction> getByAccountId(String accountId) {
        return transactions.stream()
                .filter(tx -> accountId.equals(tx.getFromAccountId()) ||
                        accountId.equals(tx.getToAccountId()))
                .collect(Collectors.toList());
    }
}
