package com.omkaar.bank.config;

import com.omkaar.bank.filter.JwtFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class FilterConfig {

    /**
     * CORS filter — order -1 means it runs BEFORE everything else,
     * including the JWT filter. This ensures OPTIONS preflight requests
     * get the correct CORS headers and are never blocked by JWT auth.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter corsFilter) {
        FilterRegistrationBean<CorsFilter> reg = new FilterRegistrationBean<>(corsFilter);
        reg.setOrder(-1); // must be lower (earlier) than JWT filter order
        return reg;
    }

    /**
     * JWT filter — order 1, runs after CORS.
     * Skips OPTIONS requests so preflight is never rejected.
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
        FilterRegistrationBean<JwtFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(jwtFilter);
        reg.addUrlPatterns("/api/*", "/api/*/*", "/api/*/*/*");
        reg.setOrder(1);
        return reg;
    }
}
