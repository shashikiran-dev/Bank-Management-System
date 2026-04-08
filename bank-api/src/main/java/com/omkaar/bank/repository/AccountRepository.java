package com.omkaar.bank.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omkaar.bank.entity.AccountEntity;

public interface AccountRepository
        extends JpaRepository<AccountEntity, UUID> {
}
