package com.omkaar.bank.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import com.omkaar.bank.service.BankOperations;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final BankOperations bank;

    public LoanController(BankOperations bank) {
        this.bank = bank;
    }

    @PostMapping("/request")
    public void requestLoan(
            @RequestParam UUID accountId,
            @RequestParam BigDecimal amount) {

        bank.requestLoan(accountId, amount);
    }

    @PostMapping("/process-next")
    public void processNextLoan() {
        bank.processNextLoan();
    }
}
