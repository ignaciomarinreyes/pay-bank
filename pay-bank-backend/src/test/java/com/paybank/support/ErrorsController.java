package com.paybank.support;

import com.paybank.account.exception.AccountNotFoundException;
import com.paybank.payment.exception.InsufficientBalanceException;
import com.paybank.payment.exception.InvalidPaymentAmountException;
import com.paybank.payment.exception.RecipientNotFoundException;
import com.paybank.payment.exception.SelfPaymentException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Throws each business exception, to verify how they are answered. Only exists in tests. */
@RestController
public class ErrorsController {

    @GetMapping("/api/test/errors/{type}")
    public String fail(@PathVariable String type) {
        switch (type) {
            case "recipient" -> throw new RecipientNotFoundException();
            case "self" -> throw new SelfPaymentException();
            case "balance" -> throw new InsufficientBalanceException();
            case "account" -> throw new AccountNotFoundException();
            case "amount" -> throw new InvalidPaymentAmountException();
            default -> throw new IllegalStateException("unexpected");
        }
    }
}
