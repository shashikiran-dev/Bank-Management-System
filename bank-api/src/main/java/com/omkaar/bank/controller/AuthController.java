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
        this.userRepo    = userRepo;
        this.accountRepo = accountRepo;
        this.otpStore    = otpStore;
        this.emailService = emailService;
        this.jwtUtil     = jwtUtil;
        this.bank        = bank;
    }

    /* ── REGISTER ─────────────────────────────────────────────────────
       Frontend sends: { name, email, password, initialDeposit, accountType? }
       ─────────────────────────────────────────────────────────────── */
    @PostMapping("/register")
    public ResponseEntity<Map<String,Object>> register(
            @RequestBody Map<String,Object> body) {

        String name     = str(body, "name");
        String email    = str(body, "email");
        String password = str(body, "password");
        BigDecimal initialDeposit = new BigDecimal(
                body.getOrDefault("initialDeposit", "0").toString());
        AccountType type = AccountType.valueOf(
                body.getOrDefault("accountType", "SAVINGS").toString().toUpperCase());

        if (userRepo.findByEmail(email).isPresent())
            return bad("Email already registered.");
        if (password.length() < 6)
            return bad("Password must be at least 6 characters.");

        Account account = AccountFactory.createAccount(type, initialDeposit);
        bank.registerAccount(account);

        UUID userId    = UUID.randomUUID();
        UUID accountId = UUID.fromString(account.getAccountId());

        UserEntity user = new UserEntity(userId, name, email,
                PasswordUtil.hash(password), accountId, Instant.now());
        userRepo.save(user);

        String token = jwtUtil.generate(userId.toString(), email, "USER");

        try { emailService.sendWelcome(email, firstName(name), shortAccNo(accountId)); }
        catch (Exception ignored) {}

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",       "success");
        res.put("token",        token);
        res.put("id",           userId.toString());
        res.put("name",         name);
        res.put("email",        email);
        res.put("account_number", shortAccNo(accountId));
        res.put("balance",      initialDeposit);
        res.put("account_type", type.name());
        return ResponseEntity.ok(res);
    }

    /* ── LOGIN Step 1 — password → send OTP ──────────────────────────
       Frontend sends: { email, password }
       ─────────────────────────────────────────────────────────────── */
    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(
            @RequestBody Map<String,Object> body) {

        String email    = str(body, "email");
        String password = str(body, "password");

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

    /* ── LOGIN Step 2 — OTP → JWT ─────────────────────────────────────
       Frontend sends: { email, otp }
       ─────────────────────────────────────────────────────────────── */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String,Object>> verifyOtp(
            @RequestBody Map<String,Object> body) {

        String email = str(body, "email");
        String otp   = str(body, "otp");

        OtpStore.Result result = otpStore.validate(email, otp);
        if (result != OtpStore.Result.OK)
            return ResponseEntity.status(401).body(error(result.message()));

        UserEntity    user    = userRepo.findByEmail(email).orElseThrow();
        AccountEntity account = accountRepo.findById(user.getAccountId()).orElseThrow();

        String token = jwtUtil.generate(user.getId().toString(), email, "USER");

        try { emailService.sendLoginAlert(email, firstName(user.getName())); }
        catch (Exception ignored) {}

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",         "success");
        res.put("token",          token);
        res.put("id",             user.getId().toString());
        res.put("name",           user.getName());
        res.put("email",          user.getEmail());
        res.put("account_number", shortAccNo(user.getAccountId()));
        res.put("balance",        account.getBalance());
        res.put("account_type",   account.getType().name());
        return ResponseEntity.ok(res);
    }

    /* ── RESEND OTP ───────────────────────────────────────────────────
       Frontend sends: { email }
       ─────────────────────────────────────────────────────────────── */
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String,Object>> resendOtp(
            @RequestBody Map<String,Object> body) {

        String email = str(body, "email");
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

    /* ── GET ACCOUNT INFO (JWT protected) ─────────────────────────── */
    @GetMapping("/account")
    public ResponseEntity<Map<String,Object>> getAccount(HttpServletRequest req) {
        UUID userId = extractUserId(req);
        UserEntity    user    = userRepo.findById(userId).orElseThrow();
        AccountEntity account = accountRepo.findById(user.getAccountId()).orElseThrow();

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",         "success");
        res.put("id",             user.getId().toString());
        res.put("name",           user.getName());
        res.put("email",          user.getEmail());
        res.put("account_number", shortAccNo(user.getAccountId()));
        res.put("balance",        account.getBalance());
        res.put("frozen",         account.isFrozen());
        res.put("account_type",   account.getType().name());
        return ResponseEntity.ok(res);
    }

    /* ── DEPOSIT / WITHDRAW via POST /api/auth/account ───────────────
       Frontend sends: { action, amount } with Bearer token
       ─────────────────────────────────────────────────────────────── */
    @PostMapping("/account")
    public ResponseEntity<Map<String,Object>> accountAction(
            @RequestBody Map<String,Object> body,
            HttpServletRequest req) {

        UUID       userId = extractUserId(req);
        String     action = str(body, "action");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        UserEntity    user    = userRepo.findById(userId).orElseThrow();
        UUID          accId   = user.getAccountId();

        try {
            if      ("deposit".equalsIgnoreCase(action))  bank.deposit(accId, amount);
            else if ("withdraw".equalsIgnoreCase(action)) bank.withdraw(accId, amount);
            else return bad("Unknown action: " + action);
        } catch (RuntimeException ex) {
            return bad(ex.getMessage());
        }

        AccountEntity updated = accountRepo.findById(accId).orElseThrow();
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",     "success");
        res.put("newBalance", updated.getBalance());
        return ResponseEntity.ok(res);
    }

    /* ── CHANGE PASSWORD ──────────────────────────────────────────────
       Frontend sends: { currentPassword, newPassword } with Bearer token
       ─────────────────────────────────────────────────────────────── */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String,Object>> changePassword(
            @RequestBody Map<String,Object> body,
            HttpServletRequest req) {

        UUID       userId  = extractUserId(req);
        String     oldPwd  = str(body, "currentPassword");
        String     newPwd  = str(body, "newPassword");
        UserEntity user    = userRepo.findById(userId).orElseThrow();

        if (!PasswordUtil.matches(oldPwd, user.getPassword()))
            return bad("Current password is incorrect.");
        if (newPwd.length() < 6)
            return bad("New password must be at least 6 characters.");

        user.setPassword(PasswordUtil.hash(newPwd));
        userRepo.save(user);

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",  "success");
        res.put("message", "Password changed successfully.");
        return ResponseEntity.ok(res);
    }

    /* ── HELPERS ─────────────────────────────────────────────────────── */
    private static UUID extractUserId(HttpServletRequest req) {
        Object attr = req.getAttribute("userId");
        if (attr == null) throw new RuntimeException("Not authenticated.");
        return UUID.fromString(attr.toString());
    }

    private static String str(Map<String,Object> body, String key) {
        Object v = body.get(key);
        return v != null ? v.toString() : "";
    }

    private static String shortAccNo(UUID id) {
        return id.toString().replace("-", "").substring(22).toUpperCase();
    }

    private static String firstName(String fullName) {
        return fullName != null ? fullName.split(" ")[0] : "User";
    }

    private static Map<String,Object> error(String msg) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status",  "error");
        m.put("message", msg);
        return m;
    }

    private static ResponseEntity<Map<String,Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(error(msg));
    }
}
