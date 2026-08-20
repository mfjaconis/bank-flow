package com.jaconis.bankflow.account.controller;

import com.jaconis.bankflow.account.dto.AccountResponse;
import com.jaconis.bankflow.account.dto.BalanceResponse;
import com.jaconis.bankflow.account.entity.AccountStatus;
import com.jaconis.bankflow.account.exception.AccountAccessDeniedException;
import com.jaconis.bankflow.account.exception.AccountNotFoundException;
import com.jaconis.bankflow.account.exception.ApiExceptionHandler;
import com.jaconis.bankflow.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import(ApiExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AccountService accountService;

    @Test
    void create_ok_returns201() throws Exception {
        Instant createdAt = Instant.parse("2026-08-20T12:00:00Z");
        when(accountService.create(eq(USER_ID), any()))
                .thenReturn(new AccountResponse(ACCOUNT_ID, USER_ID, AccountStatus.ACTIVE, "BRL", createdAt));

        mockMvc.perform(post("/accounts")
                        .header(AccountController.USER_ID_HEADER, USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void create_withoutBody_returns201() throws Exception {
        Instant createdAt = Instant.parse("2026-08-20T12:00:00Z");
        when(accountService.create(eq(USER_ID), any()))
                .thenReturn(new AccountResponse(ACCOUNT_ID, USER_ID, AccountStatus.ACTIVE, "BRL", createdAt));

        mockMvc.perform(post("/accounts")
                        .header(AccountController.USER_ID_HEADER, USER_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_invalidCurrency_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
                        .header(AccountController.USER_ID_HEADER, USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currency":"br"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/accounts"));
    }

    @Test
    void create_withoutUserHeader_returns401() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Header X-User-Id é obrigatório"))
                .andExpect(jsonPath("$.path").value("/accounts"));
    }

    @Test
    void getById_ok_returns200() throws Exception {
        Instant createdAt = Instant.parse("2026-08-20T12:00:00Z");
        when(accountService.getById(ACCOUNT_ID, USER_ID))
                .thenReturn(new AccountResponse(ACCOUNT_ID, USER_ID, AccountStatus.ACTIVE, "BRL", createdAt));

        mockMvc.perform(get("/accounts/{id}", ACCOUNT_ID)
                        .header(AccountController.USER_ID_HEADER, USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(accountService.getById(ACCOUNT_ID, USER_ID)).thenThrow(new AccountNotFoundException());

        mockMvc.perform(get("/accounts/{id}", ACCOUNT_ID)
                        .header(AccountController.USER_ID_HEADER, USER_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Conta não encontrada"))
                .andExpect(jsonPath("$.path").value("/accounts/" + ACCOUNT_ID));
    }

    @Test
    void getById_whenOtherUser_returns403() throws Exception {
        when(accountService.getById(ACCOUNT_ID, USER_ID)).thenThrow(new AccountAccessDeniedException());

        mockMvc.perform(get("/accounts/{id}", ACCOUNT_ID)
                        .header(AccountController.USER_ID_HEADER, USER_ID.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acesso negado a esta conta"));
    }

    @Test
    void getBalance_ok_returns200() throws Exception {
        when(accountService.getBalance(ACCOUNT_ID, USER_ID))
                .thenReturn(new BalanceResponse(ACCOUNT_ID, new BigDecimal("0.00"), "BRL"));

        mockMvc.perform(get("/accounts/{id}/balance", ACCOUNT_ID)
                        .header(AccountController.USER_ID_HEADER, USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void getBalance_whenNotFound_returns404() throws Exception {
        when(accountService.getBalance(ACCOUNT_ID, USER_ID)).thenThrow(new AccountNotFoundException());

        mockMvc.perform(get("/accounts/{id}/balance", ACCOUNT_ID)
                        .header(AccountController.USER_ID_HEADER, USER_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
