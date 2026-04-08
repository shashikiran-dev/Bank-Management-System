package com.omkaar.bank.service;

import com.omkaar.bank.config.BankingRules;
import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.entity.TransactionEntity;
import com.omkaar.bank.exception.TransactionLimitException;
import com.omkaar.bank.model.AccountType;
import com.omkaar.bank.model.TransactionType;
import com.omkaar.bank.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Validates all banking rules before a transaction is allowed.
 * Throws TransactionLimitException with a human-readable message on any violation.
 */
@Service
public class LimitChecker {

    private final BankingRules          rules;
    private final TransactionRepository transactionRepository;

    public LimitChecker(BankingRules rules, TransactionRepository transactionRepository) {
        this.rules                = rules;
        this.transactionRepository = transactionRepository;
    }

    /* ══════════════════════════════════════════════════════
       PUBLIC CHECK METHODS — call these before each operation
       ══════════════════════════════════════════════════════ */

    /**
     * Validate a deposit attempt.
     * Rules: max deposit per transaction.
     */
    public void checkDeposit(AccountEntity account, BigDecimal amount) {
        requirePositive(amount);
        checkFrozen(account);

        if (amount.compareTo(rules.getMaxDepositPerTx()) > 0) {
            throw new TransactionLimitException(
                String.format("Deposit amount ₹%.2f exceeds the maximum allowed per transaction of ₹%.2f.",
                    amount, rules.getMaxDepositPerTx()));
        }
    }

    /**
     * Validate a withdrawal attempt.
     * Rules: max per transaction, daily limit, minimum balance.
     */
    public void checkWithdrawal(AccountEntity account, BigDecimal amount) {
        requirePositive(amount);
        checkFrozen(account);

        // Per-transaction cap
        if (amount.compareTo(rules.getMaxWithdrawalPerTx()) > 0) {
            throw new TransactionLimitException(
                String.format("Withdrawal amount ₹%.2f exceeds the per-transaction limit of ₹%.2f.",
                    amount, rules.getMaxWithdrawalPerTx()));
        }

        // Daily cumulative limit
        BigDecimal todayWithdrawals = getTodayTotal(account.getId(), TransactionType.WITHDRAWAL);
        if (todayWithdrawals.add(amount).compareTo(rules.getDailyWithdrawalLimit()) > 0) {
            BigDecimal remaining = rules.getDailyWithdrawalLimit().subtract(todayWithdrawals);
            throw new TransactionLimitException(
                String.format("Daily withdrawal limit of ₹%.2f reached. " +
                              "You have already withdrawn ₹%.2f today. Remaining: ₹%.2f.",
                    rules.getDailyWithdrawalLimit(), todayWithdrawals,
                    remaining.max(BigDecimal.ZERO)));
        }

        // Minimum balance enforcement
        BigDecimal minBalance = account.getType() == AccountType.SAVINGS
                ? rules.getSavingsMinBalance()
                : rules.getCurrentMinBalance();

        BigDecimal balanceAfter = account.getBalance().subtract(amount);
        if (balanceAfter.compareTo(minBalance) < 0) {
            throw new TransactionLimitException(
                String.format("Insufficient balance. A minimum balance of ₹%.2f must be maintained. " +
                              "Current balance: ₹%.2f, Requested: ₹%.2f.",
                    minBalance, account.getBalance(), amount));
        }
    }

    /**
     * Validate a transfer (debit side).
     * Rules: max per transaction, daily transfer limit, minimum balance.
     */
    public void checkTransfer(AccountEntity fromAccount, BigDecimal amount) {
        requirePositive(amount);
        checkFrozen(fromAccount);

        // Per-transaction cap
        if (amount.compareTo(rules.getMaxTransferPerTx()) > 0) {
            throw new TransactionLimitException(
                String.format("Transfer amount ₹%.2f exceeds the per-transaction limit of ₹%.2f.",
                    amount, rules.getMaxTransferPerTx()));
        }

        // Daily cumulative transfer limit
        BigDecimal todayTransfers = getTodayTotal(fromAccount.getId(), TransactionType.TRANSFER);
        if (todayTransfers.add(amount).compareTo(rules.getDailyTransferLimit()) > 0) {
            BigDecimal remaining = rules.getDailyTransferLimit().subtract(todayTransfers);
            throw new TransactionLimitException(
                String.format("Daily transfer limit of ₹%.2f reached. " +
                              "Already transferred ₹%.2f today. Remaining: ₹%.2f.",
                    rules.getDailyTransferLimit(), todayTransfers,
                    remaining.max(BigDecimal.ZERO)));
        }

        // Minimum balance enforcement (same as withdrawal)
        BigDecimal minBalance = fromAccount.getType() == AccountType.SAVINGS
                ? rules.getSavingsMinBalance()
                : rules.getCurrentMinBalance();

        BigDecimal balanceAfter = fromAccount.getBalance().subtract(amount);
        if (balanceAfter.compareTo(minBalance) < 0) {
            throw new TransactionLimitException(
                String.format("Insufficient balance for transfer. Minimum balance of ₹%.2f required. " +
                              "Current: ₹%.2f, Requested: ₹%.2f.",
                    minBalance, fromAccount.getBalance(), amount));
        }
    }

    /* ══════════════════════════════════════════════════════
       PRIVATE HELPERS
       ══════════════════════════════════════════════════════ */

    /**
     * Sum all transactions of a given type that originated from this account today.
     */
    private BigDecimal getTodayTotal(UUID accountId, TransactionType type) {
        LocalDate today    = LocalDate.now(ZoneId.systemDefault());
        Instant   startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant   endOfDay   = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TransactionEntity> all =
                transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId);

        return all.stream()
                .filter(tx -> tx.getType() == type)
                .filter(tx -> accountId.equals(tx.getFromAccountId())) // only debits
                .filter(tx -> !tx.getTimestamp().isBefore(startOfDay)
                           && tx.getTimestamp().isBefore(endOfDay))
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void checkFrozen(AccountEntity account) {
        if (account.isFrozen()) {
            throw new TransactionLimitException(
                "Account is frozen. Please contact support to unfreeze your account.");
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionLimitException("Amount must be greater than zero.");
        }
    }
}
