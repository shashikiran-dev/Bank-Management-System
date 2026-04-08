package com.omkaar.bank.controller;

import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.model.Account;
import com.omkaar.bank.model.AccountType;
import com.omkaar.bank.repository.AccountRepository;
import com.omkaar.bank.repository.UserRepository;
import com.omkaar.bank.service.AccountFactory;
import com.omkaar.bank.service.BankOperations;
import com.omkaar.bank.service.EmailService;
import com.omkaar.bank.util.JwtUtil;
import com.omkaar.bank.util.OtpStore;
import com.omkaar.bank.util.PasswordUtil;
import com.omkaar.bank.mapper.AccountMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository    userRepo;
    private final AccountRepository accountRepo;
    private final OtpStore          otpStore;
    private final EmailService      emailService;
    private final JwtUtil           jwtUtil;
    private final BankOperations    bank;

    public AuthController(UserRepository userRepo,
                          AccountRepository accountRepo,
                          OtpStore otpStore,
                          EmailService emailService,
                          JwtUtil jwtUtil,
                          BankOperations bank) {
        this.userRepo     = userRepo;
        this.accountRepo  = accountRepo;
        this.otpStore     = otpStore;
        this.emailService = emailService;
        this.jwtUtil      = jwtUtil;
        this.bank         = bank;
    }

    /* ── REGISTER ─────────────────────────────────────────────────────── */
    @PostMapping("/register")
    public ResponseEntity<Map<String,Object>> register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "0") BigDecimal initialDeposit,
            @RequestParam(defaultValue = "SAVINGS") AccountType accountType) {

        if (userRepo.findByEmail(email).isPresent())
            return bad("Email already registered.");
        if (password.length() < 6)
            return bad("Password must be at least 6 characters.");

        // Use existing AccountFactory + BankOperations (respects all rules)
        Account account = AccountFactory.createAccount(accountType, initialDeposit);
        bank.registerAccount(account);

        UUID userId    = UUID.randomUUID();
        UUID accountId = UUID.fromString(account.getAccountId());

        UserEntity user = new UserEntity(
                userId, name, email,
                PasswordUtil.hash(password),
                accountId, Instant.now());
        userRepo.save(user);

        // Issue JWT immediately — user is logged in after registration
        String token = jwtUtil.generate(userId.toString(), email, "USER");

        try { emailService.sendWelcome(email, firstName(name), shortAccNo(accountId)); }
        catch (Exception ignored) {}

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",    "success");
        res.put("token",     token);
        res.put("id",        userId.toString());
        res.put("name",      name);
        res.put("email",     email);
        res.put("account_number", shortAccNo(accountId));
        res.put("balance",   initialDeposit);
        res.put("account_type", accountType.name());
        return ResponseEntity.ok(res);
    }

    /* ── LOGIN Step 1 — verify password, send OTP ────────────────────── */
    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(
            @RequestParam String email,
            @RequestParam String password) {

        Optional<UserEntity> opt = userRepo.findByEmail(email);
        if (opt.isEmpty() || !PasswordUtil.matches(password, opt.get().getPassword()))
            return ResponseEntity.status(401).body(error("Invalid email or password."));

        UserEntity user = opt.get();

        // Check account not frozen
        accountRepo.findById(user.getAccountId()).ifPresent(acc -> {
            if (acc.isFrozen())
                throw new RuntimeException("Account is frozen. Contact support.");
        });

        String otp = otpStore.generate(email);
        try { emailService.sendOtp(email, firstName(user.getName()), otp); }
        catch (Exception ex) {
            System.err.println("[OTP] Email failed: " + ex.getMessage());
            System.out.println("[OTP-DEV] OTP for " + email + " → " + otp);
        }

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status", "otp_sent");
        res.put("email",  email);
        return ResponseEntity.ok(res);
    }

    /* ── LOGIN Step 2 — verify OTP, return JWT ───────────────────────── */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String,Object>> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp) {

        OtpStore.Result result = otpStore.validate(email, otp);
        if (result != OtpStore.Result.OK)
            return ResponseEntity.status(401).body(error(result.message()));

        UserEntity    user    = userRepo.findByEmail(email).orElseThrow();
        AccountEntity account = accountRepo.findById(user.getAccountId()).orElseThrow();

        String token = jwtUtil.generate(user.getId().toString(), email, "USER");

        try { emailService.sendLoginAlert(email, firstName(user.getName())); }
        catch (Exception ignored) {}

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",       "success");
        res.put("token",        token);
        res.put("id",           user.getId().toString());
        res.put("name",         user.getName());
        res.put("email",        user.getEmail());
        res.put("account_number", shortAccNo(user.getAccountId()));
        res.put("balance",      account.getBalance());
        res.put("account_type", account.getType().name());
        return ResponseEntity.ok(res);
    }

    /* ── RESEND OTP ───────────────────────────────────────────────────── */
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String,Object>> resendOtp(@RequestParam String email) {
        Optional<UserEntity> opt = userRepo.findByEmail(email);
        if (opt.isEmpty()) return bad("Email not found.");

        otpStore.invalidate(email);
        String otp = otpStore.generate(email);
        try { emailService.sendOtp(email, firstName(opt.get().getName()), otp); }
        catch (Exception ex) {
            System.out.println("[OTP-DEV] Resent OTP for " + email + " → " + otp);
        }

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",  "success");
        res.put("message", "OTP resent.");
        return ResponseEntity.ok(res);
    }

    /* ── GET ACCOUNT INFO (JWT protected) ────────────────────────────── */
    @GetMapping("/account")
    public ResponseEntity<Map<String,Object>> getAccount(HttpServletRequest req) {
        UUID userId = extractUserId(req);
        UserEntity    user    = userRepo.findById(userId).orElseThrow();
        AccountEntity account = accountRepo.findById(user.getAccountId()).orElseThrow();

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",       "success");
        res.put("id",           user.getId().toString());
        res.put("name",         user.getName());
        res.put("email",        user.getEmail());
        res.put("account_number", shortAccNo(user.getAccountId()));
        res.put("balance",      account.getBalance());
        res.put("frozen",       account.isFrozen());
        res.put("account_type", account.getType().name());
        return ResponseEntity.ok(res);
    }

    /* ── CHANGE PASSWORD (JWT protected) ─────────────────────────────── */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String,Object>> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            HttpServletRequest req) {

        UUID       userId = extractUserId(req);
        UserEntity user   = userRepo.findById(userId).orElseThrow();

        if (!PasswordUtil.matches(currentPassword, user.getPassword()))
            return bad("Current password is incorrect.");
        if (newPassword.length() < 6)
            return bad("New password must be at least 6 characters.");

        user.setPassword(PasswordUtil.hash(newPassword));
        userRepo.save(user);
        return ResponseEntity.ok(success());
    }

    /* ── HELPERS ─────────────────────────────────────────────────────── */
    private static UUID extractUserId(HttpServletRequest req) {
        return UUID.fromString((String) req.getAttribute("userId"));
    }

    private static String shortAccNo(UUID id) {
        return id.toString().replace("-", "").substring(22).toUpperCase();
    }

    private static String firstName(String fullName) {
        return fullName.split(" ")[0];
    }

    private static Map<String,Object> error(String msg) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status",  "error");
        m.put("message", msg);
        return m;
    }

    private static Map<String,Object> success() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status", "success");
        return m;
    }

    private static ResponseEntity<Map<String,Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(error(msg));
    }
}
