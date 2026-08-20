package com.jaconis.bankflow.auth.exception;

public class AccountProvisioningException extends ApiException {

    public AccountProvisioningException() {
        super("Não foi possível criar a conta do usuário");
    }

    public AccountProvisioningException(Throwable cause) {
        super("Não foi possível criar a conta do usuário");
        initCause(cause);
    }
}
