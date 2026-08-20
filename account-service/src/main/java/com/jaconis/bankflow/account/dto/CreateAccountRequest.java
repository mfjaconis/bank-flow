package com.jaconis.bankflow.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Dados para abertura de conta")
public record CreateAccountRequest(
        @Schema(example = "BRL", description = "Código ISO 4217 da moeda. Padrão: BRL")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Moeda deve ser um código ISO 4217 (ex.: BRL)")
        String currency
) {
    public String currencyOrDefault() {
        return (currency == null || currency.isBlank()) ? "BRL" : currency;
    }
}
