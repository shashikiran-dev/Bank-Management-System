package com.omkaar.bank.filter;

import com.omkaar.bank.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    // Paths that do NOT require a token
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/verify-otp",
            "/api/auth/register",
            "/api/auth/resend-otp",
            "/api/admin/login"
    );

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true; // ← this was missing
    }
    String path = request.getRequestURI();
    return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtUtil.validate(token);
            String role   = claims.get("role", String.class);

            // For ADMIN tokens the subject is "admin" (not a UUID).
            // Set userId only for USER tokens so controllers don't crash.
            if ("ADMIN".equals(role)) {
                request.setAttribute("role",  role);
                request.setAttribute("email", claims.get("email", String.class));
                // do NOT set userId — admin controllers don't need it
            } else {
                request.setAttribute("userId", claims.getSubject());
                request.setAttribute("role",   role);
                request.setAttribute("email",  claims.get("email", String.class));
            }

            chain.doFilter(request, response);
        } catch (JwtException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token expired or invalid. Please log in again.\"}");
        }
    }
}
