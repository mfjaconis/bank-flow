package com.jaconis.bankflow.account.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    @Test
    void newAccount_startsActiveWithZeroBalance() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Account account = new Account(userId, "BRL");

        assertEquals(userId, account.getUserId());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(BigDecimal.ZERO, account.getBalance());
        assertEquals("BRL", account.getCurrency());
        assertTrue(account.isOwnedBy(userId));
        assertFalse(account.isOwnedBy(UUID.fromString("33333333-3333-3333-3333-333333333333")));
    }

    @Test
    void block_and_activate_changeStatus() {
        Account account = new Account(UUID.randomUUID(), "BRL");

        account.block();
        assertEquals(AccountStatus.BLOCKED, account.getStatus());

        account.activate();
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }
}
