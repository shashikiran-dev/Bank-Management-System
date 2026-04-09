package com.omkaar.bank.model;

import java.math.BigDecimal;

public record RegisterRequest(
    String name,
    String email,
    String password,
    BigDecimal initialDeposit,
    String accountType
) {}