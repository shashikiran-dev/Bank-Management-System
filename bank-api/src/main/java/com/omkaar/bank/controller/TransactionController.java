package com.omkaar.bank.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.omkaar.bank.model.TransactionView;
import com.omkaar.bank.service.BankOperations;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final BankOperations bank;

    public TransactionController(BankOperations bank) {
        this.bank = bank;
    }

    @GetMapping("/{accountId}")
    public List<TransactionView> getTransactionHistory(
            @PathVariable UUID accountId) {

        return bank.getTransactionHistory(accountId);
    }

    @PostMapping("/undo")
    public void undoLastTransaction() {
        bank.undoLastTransaction();
    }
}
