package com.paybank.payment.exception;

import com.paybank.exception.BusinessRuleException;

public class RecipientNotFoundException extends BusinessRuleException {

    public RecipientNotFoundException() {
        super("RECIPIENT_NOT_FOUND", "No user exists with that alias");
    }
}
