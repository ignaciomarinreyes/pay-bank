package com.paybank.account.exception;

import com.paybank.exception.BusinessRuleException;

/** The authenticated user has no account. */
public class AccountNotFoundException extends BusinessRuleException {

    public AccountNotFoundException() {
        super("ACCOUNT_NOT_FOUND", "The user has no account");
    }
}
