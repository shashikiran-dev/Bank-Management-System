package com.omkaar.bank.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omkaar.bank.entity.TransactionEntity;

public interface TransactionRepository
        extends JpaRepository<TransactionEntity, UUID> {

    List<TransactionEntity> findByFromAccountIdOrToAccountId(
            UUID fromAccountId,
            UUID toAccountId);

    Optional<TransactionEntity> findTopByOrderByTimestampDesc();
}
