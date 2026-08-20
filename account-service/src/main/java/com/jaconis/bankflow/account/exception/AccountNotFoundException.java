package com.jaconis.bankflow.account.exception;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException() {
        super("Conta não encontrada");
    }
}
