package com.omkaar.bank.controller;

import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.repository.AccountRepository;
import com.omkaar.bank.repository.UserRepository;
import com.omkaar.bank.service.BankOperations;
import com.omkaar.bank.service.LimitChecker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/transfer")
@CrossOrigin(origins = "*")
public class TransferController {

    private final UserRepository    userRepo;
    private final AccountRepository accountRepo;
    private final BankOperations    bank;
    private final LimitChecker      limitChecker;

    public TransferController(UserRepository userRepo,
                              AccountRepository accountRepo,
                              BankOperations bank,
                              LimitChecker limitChecker) {
        this.userRepo     = userRepo;
        this.accountRepo  = accountRepo;
        this.bank         = bank;
        this.limitChecker = limitChecker;
    }

    /* ── LOOKUP receiver by short account number ─────────────────────── */
    @GetMapping
    public ResponseEntity<Map<String,Object>> lookup(
            @RequestParam String accno) {

        Optional<AccountEntity> match = accountRepo.findAll().stream()
                .filter(a -> shortAccNo(a.getId()).equalsIgnoreCase(accno))
                .findFirst();

        if (match.isEmpty()) {
            Map<String,Object> err = new LinkedHashMap<>();
            err.put("status",  "error");
            err.put("message", "Account not found.");
            return ResponseEntity.ok(err);
        }

        AccountEntity account = match.get();
        Optional<UserEntity> userOpt = userRepo.findByAccountId(account.getId());

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",         "success");
        res.put("id",             userOpt.map(u -> u.getId().toString()).orElse(null));
        res.put("name",           userOpt.map(UserEntity::getName).orElse("Unknown"));
        res.put("account_number", shortAccNo(account.getId()));
        return ResponseEntity.ok(res);
    }

    /* ── EXECUTE TRANSFER ────────────────────────────────────────────── */
    @PostMapping
    public ResponseEntity<Map<String,Object>> transfer(
            @RequestParam String  receiverAccNo,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "Fund transfer") String note,
            // Legacy support — senderId param OR JWT userId attribute
            @RequestParam(required = false) UUID senderId,
            HttpServletRequest req) {

        UUID userId = resolveUserId(req, senderId);

        Optional<UserEntity> senderOpt = userRepo.findById(userId);
        if (senderOpt.isEmpty()) return bad("Sender not found.");

        UUID fromAccountId = senderOpt.get().getAccountId();

        Optional<AccountEntity> receiverAccount = accountRepo.findAll().stream()
                .filter(a -> shortAccNo(a.getId()).equalsIgnoreCase(receiverAccNo))
                .findFirst();
        if (receiverAccount.isEmpty()) return bad("Receiver account not found.");

        UUID toAccountId = receiverAccount.get().getId();
        if (fromAccountId.equals(toAccountId)) return bad("Cannot transfer to your own account.");

        AccountEntity fromAccount = accountRepo.findById(fromAccountId).orElseThrow();
        limitChecker.checkTransfer(fromAccount, amount);

        try {
            bank.transfer(fromAccountId, toAccountId, amount);
        } catch (RuntimeException ex) {
            return bad(ex.getMessage());
        }

        AccountEntity updated = accountRepo.findById(fromAccountId).orElseThrow();
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",     "success");
        res.put("newBalance", updated.getBalance());
        return ResponseEntity.ok(res);
    }

    /* ── HELPERS ─────────────────────────────────────────────────────── */
    private static UUID resolveUserId(HttpServletRequest req, UUID fallback) {
        String fromJwt = (String) req.getAttribute("userId");
        if (fromJwt != null) return UUID.fromString(fromJwt);
        if (fallback != null) return fallback;
        throw new RuntimeException("User not authenticated.");
    }

    private static String shortAccNo(UUID id) {
        return id.toString().replace("-", "").substring(22).toUpperCase();
    }

    private static ResponseEntity<Map<String,Object>> bad(String msg) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status",  "error");
        m.put("message", msg);
        return ResponseEntity.badRequest().body(m);
    }
}
