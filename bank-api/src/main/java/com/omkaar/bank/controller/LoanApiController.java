package com.omkaar.bank.controller;

import com.omkaar.bank.entity.LoanRequestEntity;
import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.model.LoanStatus;
import com.omkaar.bank.repository.LoanRequestRepository;
import com.omkaar.bank.repository.UserRepository;
import com.omkaar.bank.service.BankOperations;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "*")
public class LoanApiController {

    private final UserRepository        userRepo;
    private final LoanRequestRepository loanRepo;
    private final BankOperations        bank;

    public LoanApiController(UserRepository userRepo,
                             LoanRequestRepository loanRepo,
                             BankOperations bank) {
        this.userRepo  = userRepo;
        this.loanRepo  = loanRepo;
        this.bank      = bank;
    }

    /* ── APPLY — Frontend: POST /api/loans ──────────────────────────── */
    @PostMapping
    public ResponseEntity<Map<String,Object>> apply(
            @RequestBody Map<String,Object> body,
            HttpServletRequest req) {

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String purpose    = body.getOrDefault("purpose", "Personal Loan").toString();
        int termMonths    = body.containsKey("termMonths")
                            ? Integer.parseInt(body.get("termMonths").toString()) : 12;

        UUID resolvedId = resolveUserId(req);
        Optional<UserEntity> opt = userRepo.findById(resolvedId);
        if (opt.isEmpty()) return bad("User not found.");

        if (amount.compareTo(new BigDecimal("1000")) < 0)
            return bad("Minimum loan amount is ₹1,000.");
        if (amount.compareTo(new BigDecimal("1000000")) > 0)
            return bad("Maximum loan amount is ₹10,00,000.");
        if (termMonths < 3 || termMonths > 60)
            return bad("Loan term must be between 3 and 60 months.");

        UUID accountId = opt.get().getAccountId();

        boolean alreadyPending = loanRepo
                .findByStatusOrderByCreatedAtAsc(LoanStatus.PENDING)
                .stream().anyMatch(l -> l.getAccountId().equals(accountId));
        if (alreadyPending)
            return bad("You already have a pending loan application.");

        try {
            bank.requestLoan(accountId, amount);
        } catch (RuntimeException ex) {
            return bad(ex.getMessage());
        }

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",  "success");
        res.put("message", "Loan application submitted! It will be reviewed shortly.");
        return ResponseEntity.ok(res);
    }

    /* ── MY LOANS — Frontend: GET /api/loans ────────────────────────── */
    @GetMapping
    public ResponseEntity<Map<String,Object>> myLoans(HttpServletRequest req) {

        UUID resolvedId = resolveUserId(req);
        Optional<UserEntity> opt = userRepo.findById(resolvedId);
        if (opt.isEmpty()) return bad("User not found.");

        UUID accountId = opt.get().getAccountId();

        List<Map<String,Object>> list = loanRepo.findAll().stream()
                .filter(l -> l.getAccountId().equals(accountId))
                .sorted(Comparator.comparing(LoanRequestEntity::getCreatedAt).reversed())
                .map(l -> {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("id",           l.getId().toString());
                    m.put("amount",       l.getAmount());
                    m.put("status",       l.getStatus().name());
                    m.put("created_at",   l.getCreatedAt().toString());
                    m.put("processed_at", l.getProcessedAt() != null
                                          ? l.getProcessedAt().toString() : null);
                    if (l.getStatus() == LoanStatus.APPROVED)
                        m.put("emi", computeEmi(l.getAmount(), 12, 12).toPlainString());
                    return m;
                }).collect(Collectors.toList());

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status", "success");
        res.put("loans",  list);
        return ResponseEntity.ok(res);
    }

    /* ── EMI CALCULATOR — Frontend: GET /api/loans/emi ─────────────── */
    @GetMapping("/emi")
    public ResponseEntity<Map<String,Object>> emiCalc(
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "12") int termMonths,
            @RequestParam(defaultValue = "12") double ratePercent) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) return bad("Amount must be positive.");

        BigDecimal emi           = computeEmi(amount, ratePercent, termMonths);
        BigDecimal totalPayable  = emi.multiply(BigDecimal.valueOf(termMonths))
                                      .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest = totalPayable.subtract(amount)
                                               .setScale(2, RoundingMode.HALF_UP);

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("status",        "success");
        res.put("emi",           emi.toPlainString());
        res.put("totalPayable",  totalPayable.toPlainString());
        res.put("totalInterest", totalInterest.toPlainString());
        res.put("termMonths",    termMonths);
        res.put("ratePercent",   ratePercent);
        return ResponseEntity.ok(res);
    }

    /* ── HELPERS ─────────────────────────────────────────────────────── */
    private static BigDecimal computeEmi(BigDecimal principal,
                                          double annualRate, int months) {
        double r   = annualRate / 12.0 / 100.0;
        double p   = principal.doubleValue();
        double pow = Math.pow(1 + r, months);
        double emi = (p * r * pow) / (pow - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    private static UUID resolveUserId(HttpServletRequest req) {
        String fromJwt = (String) req.getAttribute("userId");
        if (fromJwt != null) return UUID.fromString(fromJwt);
        throw new RuntimeException("User not authenticated.");
    }

    private static ResponseEntity<Map<String,Object>> bad(String msg) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("status",  "error");
        m.put("message", msg);
        return ResponseEntity.badRequest().body(m);
    }
}
