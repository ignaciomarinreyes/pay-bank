package com.paybank.payment.exception;

import com.paybank.exception.BusinessRuleException;

public class SelfPaymentException extends BusinessRuleException {

    public SelfPaymentException() {
        super("SELF_PAYMENT", "A user cannot pay themselves");
    }
}
