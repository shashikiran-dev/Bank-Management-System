package com.omkaar.bank.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store.
 *
 * Each OTP is tied to an email, expires in 5 minutes,
 * and is invalidated immediately after one successful use.
 */
@Component
public class OtpStore {

    private static final int    OTP_LENGTH_BOUND = 1_000_000; // 000000 – 999999
    private static final long   EXPIRY_SECONDS   = 300;       // 5 minutes
    private static final int    MAX_ATTEMPTS     = 5;

    private final SecureRandom rng = new SecureRandom();

    private record OtpEntry(String code, Instant expiresAt, int attempts) {}

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    /** Generate a new 6-digit OTP for the given email and return it. */
    public String generate(String email) {
        String code = String.format("%06d", rng.nextInt(OTP_LENGTH_BOUND));
        store.put(email.toLowerCase(),
                new OtpEntry(code, Instant.now().plusSeconds(EXPIRY_SECONDS), 0));
        return code;
    }

    /**
     * Validate a submitted OTP.
     * Returns a Result describing why it failed (or that it succeeded).
     */
    public Result validate(String email, String submittedCode) {
        OtpEntry entry = store.get(email.toLowerCase());

        if (entry == null)                              return Result.NOT_FOUND;
        if (Instant.now().isAfter(entry.expiresAt()))  { store.remove(email); return Result.EXPIRED; }
        if (entry.attempts() >= MAX_ATTEMPTS)           { store.remove(email); return Result.TOO_MANY_ATTEMPTS; }

        if (!entry.code().equals(submittedCode)) {
            // increment attempt counter
            store.put(email.toLowerCase(),
                    new OtpEntry(entry.code(), entry.expiresAt(), entry.attempts() + 1));
            return Result.WRONG_CODE;
        }

        store.remove(email.toLowerCase()); // one-time use
        return Result.OK;
    }

    /** Discard any existing OTP for this email (e.g. on resend). */
    public void invalidate(String email) {
        store.remove(email.toLowerCase());
    }

    public enum Result {
        OK,
        NOT_FOUND,
        EXPIRED,
        WRONG_CODE,
        TOO_MANY_ATTEMPTS;

        public String message() {
            return switch (this) {
                case OK               -> "Verified.";
                case NOT_FOUND        -> "No OTP found. Please request a new one.";
                case EXPIRED          -> "OTP has expired. Please request a new one.";
                case WRONG_CODE       -> "Incorrect OTP. Please try again.";
                case TOO_MANY_ATTEMPTS-> "Too many incorrect attempts. Please request a new OTP.";
            };
        }
    }
}
