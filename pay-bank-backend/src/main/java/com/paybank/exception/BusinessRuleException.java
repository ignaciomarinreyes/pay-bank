package com.paybank.exception;

/** A business rule was violated. Answered with 422 and a stable error code. */
public abstract class BusinessRuleException extends RuntimeException {

    private final String code;

    protected BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
