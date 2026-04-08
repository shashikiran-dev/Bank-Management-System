package com.omkaar.bank.util;

import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * ONE-TIME migration: re-hashes any plain-text passwords that were saved
 * before BCrypt was introduced.
 *
 * It detects plain-text by checking if the stored value does NOT start
 * with the BCrypt prefix "$2a$" or "$2b$".
 *
 * ── HOW TO USE ──────────────────────────────────────────────────────────
 * 1. Add this file to your project.
 * 2. Start the app ONCE with: --spring.profiles.active=migrate-passwords
 *    e.g.  mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=migrate-passwords"
 * 3. Once complete, remove this file (or leave it — it won't run again
 *    because all hashes will already start with $2a$).
 * ────────────────────────────────────────────────────────────────────────
 */
@Configuration
@Profile("migrate-passwords")
public class PasswordMigration {

    @Bean
    public CommandLineRunner migratePasswords(UserRepository userRepository) {
        return args -> {
            System.out.println("=== PASSWORD MIGRATION STARTING ===");
            int migrated = 0;

            for (UserEntity user : userRepository.findAll()) {
                String stored = user.getPassword();

                // Skip if already hashed
                if (stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$"))) {
                    continue;
                }

                // Re-hash the plain-text value
                user.setPassword(PasswordUtil.hash(stored));
                userRepository.save(user);
                migrated++;
                System.out.println("  Migrated: " + user.getEmail());
            }

            System.out.println("=== MIGRATION COMPLETE — " + migrated + " user(s) updated ===");
        };
    }
}
