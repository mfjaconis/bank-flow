package com.jaconis.bankflow.account.exception;

public class MissingUserHeaderException extends ApiException {

    public MissingUserHeaderException() {
        super("Header X-User-Id é obrigatório");
    }
}
