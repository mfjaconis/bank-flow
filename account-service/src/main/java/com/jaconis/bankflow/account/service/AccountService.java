package com.jaconis.bankflow.account.service;

import com.jaconis.bankflow.account.dto.AccountResponse;
import com.jaconis.bankflow.account.dto.BalanceResponse;
import com.jaconis.bankflow.account.dto.CreateAccountRequest;
import com.jaconis.bankflow.account.entity.Account;
import com.jaconis.bankflow.account.exception.AccountAccessDeniedException;
import com.jaconis.bankflow.account.exception.AccountNotFoundException;
import com.jaconis.bankflow.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse create(UUID userId, CreateAccountRequest request) {
        Account account = new Account(userId, request.currencyOrDefault());
        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(UUID accountId, UUID requesterId) {
        Account account = findOwnedAccount(accountId, requesterId);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId, UUID requesterId) {
        Account account = findOwnedAccount(accountId, requesterId);
        return new BalanceResponse(account.getId(), account.getBalance(), account.getCurrency());
    }

    private Account findOwnedAccount(UUID accountId, UUID requesterId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);

        if (!account.isOwnedBy(requesterId)) {
            throw new AccountAccessDeniedException();
        }

        return account;
    }

    private static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getStatus(),
                account.getCurrency(),
                account.getCreatedAt()
        );
    }
}
