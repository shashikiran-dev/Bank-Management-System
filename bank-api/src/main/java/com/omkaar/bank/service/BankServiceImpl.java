package com.omkaar.bank.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.omkaar.bank.entity.AccountEntity;
import com.omkaar.bank.entity.LoanRequestEntity;
import com.omkaar.bank.entity.TransactionEntity;
import com.omkaar.bank.mapper.AccountMapper;
import com.omkaar.bank.mapper.TransactionMapper;
import com.omkaar.bank.model.Account;
import com.omkaar.bank.model.LoanStatus;
import com.omkaar.bank.model.TransactionView;
import com.omkaar.bank.repository.AccountRepository;
import com.omkaar.bank.repository.LoanRequestRepository;
import com.omkaar.bank.repository.TransactionRepository;

@Service
public class BankServiceImpl implements BankOperations {

    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LimitChecker          limitChecker;

    public BankServiceImpl(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           LoanRequestRepository loanRequestRepository,
                           LimitChecker limitChecker) {
        this.accountRepository     = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanRequestRepository = loanRequestRepository;
        this.limitChecker          = limitChecker;
    }

    /* ── REGISTER ─────────────────────────────────────────── */
    @Override
    @Transactional
    public void registerAccount(Account account) {
        accountRepository.save(AccountMapper.toEntity(account));
    }

    /* ── DEPOSIT ──────────────────────────────────────────── */
    @Override
    @Transactional
    public void deposit(UUID accountId, BigDecimal amount) {
        AccountEntity entity = findAccount(accountId);

        // ── Rule check ──
        limitChecker.checkDeposit(entity, amount);

        Account domain = AccountMapper.toDomain(entity);
        domain.deposit(amount);
        entity.setBalance(domain.getBalance());
        accountRepository.save(entity);
        transactionRepository.save(TransactionMapper.deposit(accountId, amount));
    }

    /* ── WITHDRAW ─────────────────────────────────────────── */
    @Override
    @Transactional
    public void withdraw(UUID accountId, BigDecimal amount) {
        AccountEntity entity = findAccount(accountId);

        // ── Rule check ──
        limitChecker.checkWithdrawal(entity, amount);

        Account domain = AccountMapper.toDomain(entity);
        domain.withdraw(amount);
        entity.setBalance(domain.getBalance());
        accountRepository.save(entity);
        transactionRepository.save(TransactionMapper.withdraw(accountId, amount));
    }

    /* ── TRANSFER ─────────────────────────────────────────── */
    @Override
    @Transactional
    public void transfer(UUID fromId, UUID toId, BigDecimal amount) {
        AccountEntity fromEntity = findAccount(fromId);
        AccountEntity toEntity   = findAccount(toId);

        // ── Rule check (only debit side needs rules) ──
        limitChecker.checkTransfer(fromEntity, amount);

        Account from = AccountMapper.toDomain(fromEntity);
        Account to   = AccountMapper.toDomain(toEntity);

        from.withdraw(amount);
        to.deposit(amount);

        fromEntity.setBalance(from.getBalance());
        toEntity.setBalance(to.getBalance());
        accountRepository.save(fromEntity);
        accountRepository.save(toEntity);
        transactionRepository.save(TransactionMapper.transfer(fromId, toId, amount));
    }

    /* ── TRANSACTION HISTORY ──────────────────────────────── */
    @Override
    @Transactional(readOnly = true)
    public List<TransactionView> getTransactionHistory(UUID accountId) {
        return transactionRepository
                .findByFromAccountIdOrToAccountId(accountId, accountId)
                .stream()
                .map(TransactionMapper::toView)
                .toList();
    }

    /* ── LOAN ─────────────────────────────────────────────── */
    @Override
    @Transactional
    public void requestLoan(UUID accountId, BigDecimal amount) {
        findAccount(accountId);
        loanRequestRepository.save(
            new LoanRequestEntity(UUID.randomUUID(), accountId,
                    amount, LoanStatus.PENDING, Instant.now()));
    }

    @Override
    @Transactional
    public void processNextLoan() {
        List<LoanRequestEntity> pending =
                loanRequestRepository.findByStatusOrderByCreatedAtAsc(LoanStatus.PENDING);
        if (pending.isEmpty()) return;

        LoanRequestEntity loan    = pending.get(0);
        AccountEntity     account = findAccount(loan.getAccountId());

        if (account.getBalance().compareTo(new BigDecimal("1000")) > 0) {
            loan.markApproved();
            Account domain = AccountMapper.toDomain(account);
            domain.deposit(loan.getAmount());
            account.setBalance(domain.getBalance());
            accountRepository.save(account);
            transactionRepository.save(
                TransactionMapper.deposit(loan.getAccountId(), loan.getAmount()));
        } else {
            loan.markRejected();
        }
        loanRequestRepository.save(loan);
    }

    /* ── UNDO LAST TRANSACTION ────────────────────────────── */
    @Override
    @Transactional
    public void undoLastTransaction() {
        TransactionEntity last = transactionRepository
                .findTopByOrderByTimestampDesc()
                .orElseThrow(() -> new IllegalStateException("No transactions to undo."));

        switch (last.getType()) {
            case DEPOSIT -> {
                AccountEntity acc = findAccount(last.getToAccountId());
                acc.setBalance(acc.getBalance().subtract(last.getAmount()));
                accountRepository.save(acc);
            }
            case WITHDRAWAL -> {
                AccountEntity acc = findAccount(last.getFromAccountId());
                acc.setBalance(acc.getBalance().add(last.getAmount()));
                accountRepository.save(acc);
            }
            case TRANSFER -> {
                AccountEntity from = findAccount(last.getFromAccountId());
                AccountEntity to   = findAccount(last.getToAccountId());
                from.setBalance(from.getBalance().add(last.getAmount()));
                to.setBalance(to.getBalance().subtract(last.getAmount()));
                accountRepository.save(from);
                accountRepository.save(to);
            }
        }
        transactionRepository.delete(last);
    }

    /* ── HELPERS ──────────────────────────────────────────── */
    private AccountEntity findAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    }
}
