package com.jaconis.bankflow.account.service;

import com.jaconis.bankflow.account.dto.AccountResponse;
import com.jaconis.bankflow.account.dto.BalanceResponse;
import com.jaconis.bankflow.account.dto.CreateAccountRequest;
import com.jaconis.bankflow.account.entity.Account;
import com.jaconis.bankflow.account.entity.AccountStatus;
import com.jaconis.bankflow.account.exception.AccountAccessDeniedException;
import com.jaconis.bankflow.account.exception.AccountNotFoundException;
import com.jaconis.bankflow.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    AccountService accountService;

    @Test
    void create_ok_persistsActiveAccountWithZeroBalance() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id",
                    UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return account;
        });

        AccountResponse response = accountService.create(userId, new CreateAccountRequest(null));

        assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), response.id());
        assertEquals(userId, response.userId());
        assertEquals(AccountStatus.ACTIVE, response.status());
        assertEquals("BRL", response.currency());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(BigDecimal.ZERO, saved.getBalance());
        assertEquals(AccountStatus.ACTIVE, saved.getStatus());
        assertEquals("BRL", saved.getCurrency());
    }

    @Test
    void create_withCurrency_usesProvidedCurrency() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.create(userId, new CreateAccountRequest("USD"));

        assertEquals("USD", response.currency());
    }

    @Test
    void getById_ok_returnsAccount() {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Account account = account(accountId, userId, "BRL", BigDecimal.ZERO);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getById(accountId, userId);

        assertEquals(accountId, response.id());
        assertEquals(userId, response.userId());
        assertEquals(AccountStatus.ACTIVE, response.status());
    }

    @Test
    void getById_whenNotFound_throwsNotFound() {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getById(accountId, userId));
    }

    @Test
    void getById_whenOtherUser_throwsForbidden() {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID ownerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID otherUser = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Account account = account(accountId, ownerId, "BRL", BigDecimal.ZERO);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(AccountAccessDeniedException.class,
                () -> accountService.getById(accountId, otherUser));
    }

    @Test
    void getBalance_ok_returnsBalance() {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Account account = account(accountId, userId, "BRL", new BigDecimal("150.50"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        BalanceResponse response = accountService.getBalance(accountId, userId);

        assertEquals(accountId, response.accountId());
        assertEquals(new BigDecimal("150.50"), response.balance());
        assertEquals("BRL", response.currency());
    }

    @Test
    void getBalance_whenNotFound_throwsNotFound() {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getBalance(accountId, userId));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getBalance_whenOtherUser_throwsForbidden() {
        UUID accountId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID ownerId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID otherUser = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Account account = account(accountId, ownerId, "BRL", BigDecimal.TEN);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(AccountAccessDeniedException.class,
                () -> accountService.getBalance(accountId, otherUser));
    }

    private static Account account(UUID id, UUID userId, String currency, BigDecimal balance) {
        Account account = new Account(userId, currency);
        ReflectionTestUtils.setField(account, "id", id);
        ReflectionTestUtils.setField(account, "balance", balance);
        return account;
    }
}
