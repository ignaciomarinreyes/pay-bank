package com.paybank.account.mapper;

import com.paybank.account.model.Account;
import com.paybank.generated.model.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getBalance());
    }
}
