package com.paybank.payment.exception;

import com.paybank.exception.BusinessRuleException;

public class InsufficientBalanceException extends BusinessRuleException {

    public InsufficientBalanceException() {
        super("INSUFFICIENT_BALANCE", "The balance is not enough to make the payment");
    }
}
