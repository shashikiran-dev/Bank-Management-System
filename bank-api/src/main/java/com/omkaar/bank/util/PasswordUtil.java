package com.omkaar.bank.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Stateless BCrypt helper.
 * Uses a single shared encoder instance (thread-safe).
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    // strength 12 = ~300 ms per hash on modern hardware — good balance
    private static final BCryptPasswordEncoder ENCODER =
            new BCryptPasswordEncoder(12);

    /** Hash a plain-text password. Call this on registration and password-change. */
    public static String hash(String plainText) {
        return ENCODER.encode(plainText);
    }

    /** Check a plain-text password against a stored hash. */
    public static boolean matches(String plainText, String storedHash) {
        return ENCODER.matches(plainText, storedHash);
    }
}
