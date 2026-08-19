package com.jaconis.bankflow.auth.exception;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException() {
        super("Usuário não encontrado");
    }
}
