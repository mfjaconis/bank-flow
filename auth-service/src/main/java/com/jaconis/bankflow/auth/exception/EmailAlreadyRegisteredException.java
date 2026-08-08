package com.jaconis.bankflow.auth.exception;

public class EmailAlreadyRegisteredException extends ApiException {

    public EmailAlreadyRegisteredException() {
        super("E-mail já cadastrado");
    }
}
