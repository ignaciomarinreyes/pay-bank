package com.paybank.payment.exception;

/** The amount is not greater than 0 or has more than 2 decimals. Answered with 400. */
public class InvalidPaymentAmountException extends RuntimeException {

    public InvalidPaymentAmountException() {
        super("amount must be greater than 0 and have at most 2 decimals");
    }
}
