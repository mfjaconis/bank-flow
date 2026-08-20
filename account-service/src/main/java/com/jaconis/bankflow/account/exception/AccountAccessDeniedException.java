package com.jaconis.bankflow.account.exception;

public class AccountAccessDeniedException extends ApiException {

    public AccountAccessDeniedException() {
        super("Acesso negado a esta conta");
    }
}
