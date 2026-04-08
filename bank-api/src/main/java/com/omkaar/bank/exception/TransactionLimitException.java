package com.omkaar.bank.exception;

/**
 * Thrown when a transaction violates a configured banking rule:
 * - Exceeds per-transaction amount cap
 * - Exceeds daily cumulative limit
 * - Would breach the account's minimum balance
 */
public class TransactionLimitException extends RuntimeException {

    public TransactionLimitException(String message) {
        super(message);
    }
}
