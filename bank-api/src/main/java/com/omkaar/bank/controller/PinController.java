package com.omkaar.bank.controller;

import com.omkaar.bank.entity.UserEntity;
import com.omkaar.bank.repository.UserRepository;
import com.omkaar.bank.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pin")
@CrossOrigin(origins = "*")
public class PinController {

    private final UserRepository userRepo;

    public PinController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /* ── SET PIN ──────────────────────────────────────────────────────── */
    @PostMapping("/set")
    public ResponseEntity<Map<String,Object>> setPin(
            @RequestParam String pin,
            @RequestParam(required = false) String password,
            // Legacy: explicit userId param OR JWT
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String action,
            HttpServletRequest req) {

        UUID userId = resolveUserId(req, id);

        if (pin == null || !pin.matches("\\d{4}"))
            return bad("PIN must be exactly 4 digits.");

        UserEntity user = userRepo.findById(userId).orElseThrow();

        // If password provided, verify it first
        if (password != null && !PasswordUtil.matches(password, user.getPassword()))
            return bad("Incorrect password.");

        user.setPinHash(PasswordUtil.hash(pin));
        userRepo.save(user);
        return ok(success());
    }

    /* ── VERIFY PIN ───────────────────────────────────────────────────── */
    @PostMapping("/verify")
    public ResponseEntity<Map<String,Object>> verifyPin(
            @RequestParam String pin,
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String action,
            HttpServletRequest req) {

        UUID userId = resolveUserId(req, id);
        UserEntity user = userRepo.findById(userId).orElseThrow();

        if (user.getPinHash() == null || user.getPinHash().isBlank())
            return bad("No PIN set for this account.");

        if (!PasswordUtil.matches(pin, user.getPinHash()))
            return ResponseEntity.status(401).body(error("Incorrect PIN."));

        return ok(success());
    }

    /* ── Legacy POST /api/pin with action param (backward compatibility) */
    @PostMapping
    public ResponseEntity<Map<String,Object>> handleLegacy(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID   id,
            @RequestParam(required = false) String pin,
            @RequestParam(required = false) String password,
            HttpServletRequest req) {

        if ("set".equalsIgnoreCase(action)) {
            return setPin(pin, password, id, action, req);
        }
        if ("verify".equalsIgnoreCase(action)) {
            return verifyPin(pin, id, action, req);
        }
        return bad("Unknown action: " + action);
    }

    /* ── HELPERS ─────────────────────────────────────────────────────── */
    private static UUID resolveUserId(HttpServletRequest req, UUID fallback) {
        String fromJwt = (String) req.getAttribute("userId");
        if (fromJwt != null) return UUID.fromString(fromJwt);
        if (fallback != null) return fallback;
        throw new RuntimeException("User not authenticated.");
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

    private static ResponseEntity<Map<String,Object>> ok(Map<String,Object> body) {
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<Map<String,Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(error(msg));
    }
}
