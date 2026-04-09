package com.omkaar.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Global CORS configuration registered as a Filter — runs BEFORE
 * the JWT filter and Spring Security, so OPTIONS preflight requests
 * are never blocked.
 */
@Configuration
public class GlobalCorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow all origins — restrict to your domain in production
        config.addAllowedOriginPattern("*");

        // All standard methods including OPTIONS (preflight)
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // Allow Authorization header (needed for JWT Bearer token)
        config.addAllowedHeader("*");

        // Allow credentials (cookies / Authorization header)
        config.setAllowCredentials(false); // set true only if using cookies

        // Cache preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
