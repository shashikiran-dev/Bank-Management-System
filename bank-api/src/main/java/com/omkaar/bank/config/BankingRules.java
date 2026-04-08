package com.omkaar.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * All banking rules read from application.properties under prefix "bank.rules".
 *
 * NOTE: No @Component here — registered exclusively via
 * @EnableConfigurationProperties(BankingRules.class) in CoreConfig.
 * Having both causes a duplicate bean error.
 */
@ConfigurationProperties(prefix = "bank.rules")
public class BankingRules {

    private BigDecimal savingsMinBalance   = new BigDecimal("500.00");
    private BigDecimal currentMinBalance   = new BigDecimal("0.00");
    private BigDecimal maxWithdrawalPerTx  = new BigDecimal("200000.00");
    private BigDecimal maxTransferPerTx    = new BigDecimal("200000.00");
    private BigDecimal maxDepositPerTx     = new BigDecimal("1000000.00");
    private BigDecimal dailyWithdrawalLimit = new BigDecimal("50000.00");
    private BigDecimal dailyTransferLimit   = new BigDecimal("100000.00");

    /* ── Getters ── */
    public BigDecimal getSavingsMinBalance()    { return savingsMinBalance; }
    public BigDecimal getCurrentMinBalance()    { return currentMinBalance; }
    public BigDecimal getMaxWithdrawalPerTx()   { return maxWithdrawalPerTx; }
    public BigDecimal getMaxTransferPerTx()     { return maxTransferPerTx; }
    public BigDecimal getMaxDepositPerTx()      { return maxDepositPerTx; }
    public BigDecimal getDailyWithdrawalLimit() { return dailyWithdrawalLimit; }
    public BigDecimal getDailyTransferLimit()   { return dailyTransferLimit; }

    /* ── Setters ── */
    public void setSavingsMinBalance(BigDecimal v)    { this.savingsMinBalance    = v; }
    public void setCurrentMinBalance(BigDecimal v)    { this.currentMinBalance    = v; }
    public void setMaxWithdrawalPerTx(BigDecimal v)   { this.maxWithdrawalPerTx   = v; }
    public void setMaxTransferPerTx(BigDecimal v)     { this.maxTransferPerTx     = v; }
    public void setMaxDepositPerTx(BigDecimal v)      { this.maxDepositPerTx      = v; }
    public void setDailyWithdrawalLimit(BigDecimal v) { this.dailyWithdrawalLimit = v; }
    public void setDailyTransferLimit(BigDecimal v)   { this.dailyTransferLimit   = v; }
}
