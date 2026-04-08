package com.omkaar.bank.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omkaar.bank.entity.LoanRequestEntity;
import com.omkaar.bank.model.LoanStatus;

public interface LoanRequestRepository
        extends JpaRepository<LoanRequestEntity, UUID> {

    List<LoanRequestEntity> findByStatusOrderByCreatedAtAsc(
            LoanStatus status);
}
