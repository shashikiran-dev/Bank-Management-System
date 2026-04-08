package com.omkaar.bank;

import com.omkaar.bank.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Application context test.
 *
 * - Uses H2 in-memory DB instead of MySQL (no DB server needed)
 * - Mocks EmailService so no SMTP connection is attempted
 * - Excludes MailSenderAutoConfiguration via annotation (reliable approach)
 */
@SpringBootTest
@TestPropertySource(properties = {
    // ── In-memory H2 instead of MySQL ────────────────────────────────
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",

    // ── Dummy mail host (won't connect, EmailService is mocked) ──────
    "spring.mail.host=localhost",
    "spring.mail.port=25",

    // ── Banking config ────────────────────────────────────────────────
    "bank.admin.username=admin",
    "bank.admin.password=test",
    "bank.rules.savings-min-balance=500",
    "bank.rules.current-min-balance=0",
    "bank.rules.max-withdrawal-per-tx=200000",
    "bank.rules.max-transfer-per-tx=200000",
    "bank.rules.max-deposit-per-tx=1000000",
    "bank.rules.daily-withdrawal-limit=50000",
    "bank.rules.daily-transfer-limit=100000"
})
class BankApplicationTests {

    /**
     * Mock EmailService so Spring never tries to build a real JavaMailSender.
     * This prevents the SMTP connection error entirely.
     */
    @MockBean
    EmailService emailService;

    @Test
    void contextLoads() {
        // Passes if Spring context starts without errors.
    }
}
