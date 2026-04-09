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

    // CORS is now handled globally by GlobalCorsConfig (CorsFilter bean)
    // which runs before the JWT filter — do NOT add WebMvcConfigurer here
    // as it would conflict with the filter-level CORS setup.
}
