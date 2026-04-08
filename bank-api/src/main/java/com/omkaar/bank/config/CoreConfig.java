package com.omkaar.bank.config;

import com.omkaar.bank.repository.AccountRepository;
import com.omkaar.bank.repository.LoanRequestRepository;
import com.omkaar.bank.repository.TransactionRepository;
import com.omkaar.bank.service.BankOperations;
import com.omkaar.bank.service.BankServiceImpl;
import com.omkaar.bank.service.LimitChecker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({AdminConfig.class, BankingRules.class})
public class CoreConfig {

    @Bean
    @Primary
    public BankOperations bankOperations(AccountRepository ar,
                                         TransactionRepository tr,
                                         LoanRequestRepository lr,
                                         LimitChecker limitChecker) {
        return new BankServiceImpl(ar, tr, lr, limitChecker);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }
}
