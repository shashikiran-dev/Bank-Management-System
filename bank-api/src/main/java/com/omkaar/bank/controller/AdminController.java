package com.omkaar.bank.controller;

import com.omkaar.bank.config.AdminConfig;
import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.entity.LoanRequestEntity;
import com.omkaar.bank.entity.TransactionEntity;
import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.model.LoanStatus;
import com.omkaar.bank.repository.AccountRepository;
import com.omkaar.bank.repository.LoanRequestRepository;
import com.omkaar.bank.repository.TransactionRepository;
import com.omkaar.bank.repository.UserRepository;
import com.omkaar.bank.service.BankOperations;
import com.omkaar.bank.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminConfig           adminConfig;
    private final AccountRepository     accountRepo;
    private final UserRepository        userRepo;
    private final TransactionRepository txRepo;
    private final LoanRequestRepository loanRepo;
    private final BankOperations        bank;
    private final JwtUtil               jwtUtil;

    public AdminController(AdminConfig adminConfig,
                           AccountRepository accountRepo,
                           UserRepository userRepo,
                           TransactionRepository txRepo,
                           LoanRequestRepository loanRepo,
                           BankOperations bank,
                           JwtUtil jwtUtil) {
        this.adminConfig = adminConfig;
        this.accountRepo = accountRepo;
        this.userRepo    = userRepo;
        this.txRepo      = txRepo;
        this.loanRepo    = loanRepo;
        this.bank        = bank;
        this.jwtUtil     = jwtUtil;
    }

    /* ── ADMIN LOGIN ─────────────────────────────────────────────────── */
    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> adminLogin(
            @RequestBody Map<String,String> body) {

        String username = body.get("username");
        String password = body.get("password");

        if (!adminConfig.authenticate(username, password))
            return ResponseEntity.status(401).body(error("Invalid admin credentials."));

        // Subject is "admin" — admin controllers never call UUID.fromString on it
        String token = jwtUtil.generate("admin", username, "ADMIN");

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status", "success");
        res.put("token",  token);
        res.put("role",   "ADMIN");
        return ResponseEntity.ok(res);
    }

    /* ── DASHBOARD STATS ─────────────────────────────────────────────── */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String,Object>> dashboard(HttpServletRequest req) {
        return ResponseEntity.ok(getStats());
    }

    /* ── ALL ACCOUNTS ────────────────────────────────────────────────── */
    @GetMapping("/accounts")
    public ResponseEntity<Map<String,Object>> accounts(HttpServletRequest req) {
        return ResponseEntity.ok(getAccounts());
    }

    /* ── ALL TRANSACTIONS ────────────────────────────────────────────── */
    @GetMapping("/transactions")
    public ResponseEntity<Map<String,Object>> transactions(HttpServletRequest req) {
        return ResponseEntity.ok(getAllTransactions());
    }

    /* ── ALL LOANS ───────────────────────────────────────────────────── */
    @GetMapping("/loans")
    public ResponseEntity<Map<String,Object>> loans(HttpServletRequest req) {
        return ResponseEntity.ok(getAllLoans());
    }

    /* ── LOCK ACCOUNT ────────────────────────────────────────────────── */
    @PostMapping("/accounts/{id}/lock")
    public ResponseEntity<Map<String,Object>> lockAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(setLock(id, true));
    }

    /* ── UNLOCK ACCOUNT ──────────────────────────────────────────────── */
    @PostMapping("/accounts/{id}/unlock")
    public ResponseEntity<Map<String,Object>> unlockAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(setLock(id, false));
    }

    /* ── PROCESS LOAN ────────────────────────────────────────────────── */
    @PostMapping("/loans/process")
    public ResponseEntity<Map<String,Object>> processLoan() {
        return ResponseEntity.ok(processNextLoan());
    }

    /* ── STATS ───────────────────────────────────────────────────────── */
    private Map<String,Object> getStats() {
        List<AccountEntity> all = accountRepo.findAll();
        BigDecimal totalBalance = all.stream()
                .map(AccountEntity::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingLoans = loanRepo
                .findByStatusOrderByCreatedAtAsc(LoanStatus.PENDING).size();

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",         "success");
        res.put("totalAccounts",  all.size());
        res.put("totalBalance",   totalBalance);
        res.put("lockedAccounts", all.stream().filter(AccountEntity::isFrozen).count());
        res.put("totalTx",        txRepo.count());
        res.put("pendingLoans",   pendingLoans);
        return res;
    }

    /* ── ACCOUNTS ────────────────────────────────────────────────────── */
    private Map<String,Object> getAccounts() {
        Map<UUID, AccountEntity> accountMap = accountRepo.findAll().stream()
                .collect(Collectors.toMap(AccountEntity::getId, a -> a));

        List<Map<String,Object>> list = userRepo.findAll().stream().map(u -> {
            AccountEntity acc = accountMap.get(u.getAccountId());
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id",             u.getId().toString());
            m.put("account_number", shortAccNo(u.getAccountId()));
            m.put("name",           u.getName());
            m.put("email",          u.getEmail());
            m.put("balance",        acc != null ? acc.getBalance() : BigDecimal.ZERO);
            m.put("is_locked",      acc != null && acc.isFrozen());
            m.put("account_type",   acc != null ? acc.getType().name() : "UNKNOWN");
            m.put("created_at",     u.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",   "success");
        res.put("accounts", list);
        return res;
    }

    /* ── TRANSACTIONS ────────────────────────────────────────────────── */
    private Map<String,Object> getAllTransactions() {
        Map<UUID, String> nameMap = new HashMap<>();
        userRepo.findAll().forEach(u -> nameMap.put(u.getAccountId(), u.getName()));

        List<Map<String,Object>> list = txRepo.findAll().stream()
                .sorted(Comparator.comparing(TransactionEntity::getTimestamp).reversed())
                .map(tx -> {
                    UUID refId = tx.getFromAccountId() != null
                                 ? tx.getFromAccountId() : tx.getToAccountId();
                    String fType = toFrontendType(tx);
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("id",           tx.getId().toString());
                    m.put("type",         fType);
                    m.put("amount",       tx.getAmount());
                    m.put("description",  typeLabel(fType));
                    m.put("account_name", nameMap.getOrDefault(refId, "—"));
                    m.put("created_at",   tx.getTimestamp().toString());
                    return m;
                }).collect(Collectors.toList());

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",       "success");
        res.put("transactions", list);
        return res;
    }

    /* ── LOANS ───────────────────────────────────────────────────────── */
    private Map<String,Object> getAllLoans() {
        Map<UUID, String> nameMap = new HashMap<>();
        userRepo.findAll().forEach(u -> nameMap.put(u.getAccountId(), u.getName()));

        List<Map<String,Object>> list = loanRepo.findAll().stream()
                .sorted(Comparator.comparing(LoanRequestEntity::getCreatedAt).reversed())
                .map(l -> {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("id",           l.getId().toString());
                    m.put("account_name", nameMap.getOrDefault(l.getAccountId(), "—"));
                    m.put("amount",       l.getAmount());
                    m.put("status",       l.getStatus().name());
                    m.put("created_at",   l.getCreatedAt().toString());
                    m.put("processed_at", l.getProcessedAt() != null
                                          ? l.getProcessedAt().toString() : null);
                    return m;
                }).collect(Collectors.toList());

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status", "success");
        res.put("loans",  list);
        return res;
    }

    /* ── PROCESS NEXT LOAN ───────────────────────────────────────────── */
    private Map<String,Object> processNextLoan() {
        List<LoanRequestEntity> pending =
                loanRepo.findByStatusOrderByCreatedAtAsc(LoanStatus.PENDING);
        if (pending.isEmpty()) return error("No pending loan applications.");
        try {
            bank.processNextLoan();
            Map<String,Object> res = new LinkedHashMap<>();
            res.put("status",  "success");
            res.put("message", "Loan processed successfully.");
            return res;
        } catch (Exception ex) {
            return error("Failed: " + ex.getMessage());
        }
    }

    /* ── LOCK / UNLOCK ───────────────────────────────────────────────── */
    private Map<String,Object> setLock(UUID userId, boolean lock) {
        if (userId == null) return error("accountId is required.");
        Optional<UserEntity> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) return error("User not found.");
        Optional<AccountEntity> accOpt = accountRepo.findById(userOpt.get().getAccountId());
        if (accOpt.isEmpty()) return error("Account not found.");
        AccountEntity acc = accOpt.get();
        acc.setFrozen(lock);
        accountRepo.save(acc);
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",  "success");
        res.put("message", lock ? "Account locked." : "Account unlocked.");
        return res;
    }

    /* ── HELPERS ─────────────────────────────────────────────────────── */
    private static String shortAccNo(UUID id) {
        return id.toString().replace("-", "").substring(22).toUpperCase();
    }

    private static String toFrontendType(TransactionEntity tx) {
        return switch (tx.getType()) {
            case DEPOSIT    -> "DEPOSIT";
            case WITHDRAWAL -> "WITHDRAW";
            case TRANSFER   -> "TRANSFER_OUT";
        };
    }

    private static String typeLabel(String type) {
        return switch (type) {
            case "DEPOSIT"      -> "Deposit";
            case "WITHDRAW"     -> "Withdrawal";
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
}
