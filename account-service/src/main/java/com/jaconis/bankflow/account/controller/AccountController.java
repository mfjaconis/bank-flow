package com.jaconis.bankflow.account.controller;

import com.jaconis.bankflow.account.dto.AccountResponse;
import com.jaconis.bankflow.account.dto.BalanceResponse;
import com.jaconis.bankflow.account.dto.CreateAccountRequest;
import com.jaconis.bankflow.account.dto.ErrorResponse;
import com.jaconis.bankflow.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "Abertura e consulta de contas")
public class AccountController {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Abrir conta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validação",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(
            @RequestHeader(USER_ID_HEADER) UUID userId,
            @Valid @RequestBody(required = false) CreateAccountRequest request
    ) {
        CreateAccountRequest body = request != null ? request : new CreateAccountRequest(null);
        return accountService.create(userId, body);
    }

    @Operation(summary = "Consultar conta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Conta de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public AccountResponse getById(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID userId
    ) {
        return accountService.getById(id, userId);
    }

    @Operation(summary = "Consultar saldo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "401", description = "X-User-Id ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Conta de outro usuário",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(
            @PathVariable UUID id,
            @RequestHeader(USER_ID_HEADER) UUID userId
    ) {
        return accountService.getBalance(id, userId);
    }
}
