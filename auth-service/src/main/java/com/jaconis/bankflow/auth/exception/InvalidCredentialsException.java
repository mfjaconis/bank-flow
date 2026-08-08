package com.jaconis.bankflow.auth.exception;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super("E-mail ou senha inválidos");
    }
}
