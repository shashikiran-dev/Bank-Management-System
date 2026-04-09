package com.omkaar.bank.controller;

import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.entity.TransactionEntity;
import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.repository.AccountRepository;
import com.omkaar.bank.repository.TransactionRepository;
import com.omkaar.bank.repository.UserRepository;
import com.omkaar.bank.service.PdfStatementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GET /api/statement?month=YYYY-MM
 * Reads userId from JWT — no accountId param needed from frontend.
 */
@RestController
@RequestMapping("/api/statement")
@CrossOrigin(origins = "*")
public class StatementController {

    private final UserRepository        userRepository;
    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;
    private final PdfStatementService   pdfStatementService;

    public StatementController(UserRepository userRepository,
                               AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               PdfStatementService pdfStatementService) {
        this.userRepository        = userRepository;
        this.accountRepository     = accountRepository;
        this.transactionRepository = transactionRepository;
        this.pdfStatementService   = pdfStatementService;
    }

    @GetMapping
    public ResponseEntity<?> downloadStatement(
            @RequestParam String month,          // e.g. "2024-03"
            HttpServletRequest req) {

        // ── Get userId from JWT (set by JwtFilter) ───────────────────────
        String userIdStr = (String) req.getAttribute("userId");
        if (userIdStr == null)
            return ResponseEntity.status(401).body("Not authenticated.");

        UUID userId = UUID.fromString(userIdStr);

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty())
            return ResponseEntity.badRequest().body("User not found.");

        UserEntity user = userOpt.get();
        Optional<AccountEntity> accOpt = accountRepository.findById(user.getAccountId());
        if (accOpt.isEmpty())
            return ResponseEntity.badRequest().body("Account not found.");

        AccountEntity account = accOpt.get();

        List<TransactionEntity> transactions =
                transactionRepository.findByFromAccountIdOrToAccountId(
                        account.getId(), account.getId());

        try {
            byte[] pdf = pdfStatementService.generate(user, account, transactions, month);
            String filename = "SecureBank_Statement_" + month + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdf.length))
                    .body(pdf);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Failed to generate statement: " + e.getMessage());
        }
    }
}
