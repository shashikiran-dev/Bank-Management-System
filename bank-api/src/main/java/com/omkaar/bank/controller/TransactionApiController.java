package com.omkaar.bank.controller;

import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.entity.TransactionEntity;
import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.model.TransactionType;
import com.omkaar.bank.repository.AccountRepository;
import com.omkaar.bank.repository.TransactionRepository;
import com.omkaar.bank.repository.UserRepository;
import com.omkaar.bank.service.BankOperations;
import com.omkaar.bank.service.LimitChecker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionApiController {

    private final UserRepository        userRepo;
    private final AccountRepository     accountRepo;
    private final TransactionRepository txRepo;
    private final BankOperations        bank;
    private final LimitChecker          limitChecker;

    public TransactionApiController(UserRepository userRepo,
                                    AccountRepository accountRepo,
                                    TransactionRepository txRepo,
                                    BankOperations bank,
                                    LimitChecker limitChecker) {
        this.userRepo     = userRepo;
        this.accountRepo  = accountRepo;
        this.txRepo       = txRepo;
        this.bank         = bank;
        this.limitChecker = limitChecker;
    }

    /* ── GET transaction history ─────────────────────────────────────── */
    @GetMapping
    public ResponseEntity<Map<String,Object>> getTransactions(
            @RequestParam(required = false) UUID accountId,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "100") int limit,
            HttpServletRequest req) {

        UUID userId = resolveUserId(req, accountId);
        Optional<UserEntity> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return ok(error("User not found."));

        UUID realAccountId = userOpt.get().getAccountId();

        List<Map<String,Object>> list = txRepo
                .findByFromAccountIdOrToAccountId(realAccountId, realAccountId)
                .stream()
                .sorted(Comparator.comparing(TransactionEntity::getTimestamp).reversed())
                .limit(limit)
                .filter(tx -> "ALL".equalsIgnoreCase(type)
                           || frontendType(tx, realAccountId).equalsIgnoreCase(type))
                .map(tx -> toView(tx, realAccountId))
                .collect(Collectors.toList());

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",       "success");
        res.put("transactions", list);
        return ok(res);
    }

    /* ── DEPOSIT ─────────────────────────────────────────────────────── */
    @PostMapping("/deposit")
    public ResponseEntity<Map<String,Object>> deposit(
            @RequestBody Map<String,Object> body,
            HttpServletRequest req) {

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        UUID          userId  = resolveUserId(req, null);
        AccountEntity account = findAccount(userId);
        limitChecker.checkDeposit(account, amount);
        bank.deposit(account.getId(), amount);

        AccountEntity updated = accountRepo.findById(account.getId()).orElseThrow();
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",     "success");
        res.put("newBalance", updated.getBalance());
        return ok(res);
    }

    /* ── WITHDRAW ────────────────────────────────────────────────────── */
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String,Object>> withdraw(
            @RequestBody Map<String,Object> body,
            HttpServletRequest req) {

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        UUID          userId  = resolveUserId(req, null);
        AccountEntity account = findAccount(userId);
        limitChecker.checkWithdrawal(account, amount);
        bank.withdraw(account.getId(), amount);

        AccountEntity updated = accountRepo.findById(account.getId()).orElseThrow();
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",     "success");
        res.put("newBalance", updated.getBalance());
        return ok(res);
    }

    /* ── HELPERS ─────────────────────────────────────────────────────── */

    private AccountEntity findAccount(UUID userId) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        return accountRepo.findById(user.getAccountId()).orElseThrow();
    }

    private static UUID resolveUserId(HttpServletRequest req, UUID fallback) {
        String fromJwt = (String) req.getAttribute("userId");
        if (fromJwt != null) return UUID.fromString(fromJwt);
        if (fallback != null) return fallback;
        throw new RuntimeException("User not authenticated.");
    }

    private static Map<String,Object> toView(TransactionEntity tx, UUID myAccountId) {
        String fType = frontendType(tx, myAccountId);
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",          tx.getId().toString());
        m.put("type",        fType);
        m.put("amount",      tx.getAmount());
        m.put("description", typeLabel(fType));
        m.put("created_at",  tx.getTimestamp().toString());
        return m;
    }

    private static String frontendType(TransactionEntity tx, UUID myAccountId) {
        return switch (tx.getType()) {
            case DEPOSIT    -> "DEPOSIT";
            case WITHDRAWAL -> "WITHDRAW";
            case TRANSFER   -> myAccountId.equals(tx.getToAccountId())
                               ? "TRANSFER_IN" : "TRANSFER_OUT";
        };
    }

    private static String typeLabel(String type) {
        return switch (type) {
            case "DEPOSIT"      -> "Deposit";
            case "WITHDRAW"     -> "Withdrawal";
            case "TRANSFER_IN"  -> "Transfer Received";
            case "TRANSFER_OUT" -> "Transfer Sent";
            default             -> type;
        };
    }

    private static Map<String,Object> error(String msg) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status",  "error");
        m.put("message", msg);
        return m;
    }

    private static ResponseEntity<Map<String,Object>> ok(Map<String,Object> body) {
        return ResponseEntity.ok(body);
    }
}
