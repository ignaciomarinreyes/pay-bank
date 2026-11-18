package com.paybank.account.controller;

import com.paybank.account.service.AccountService;
import com.paybank.common.web.PageQuery;
import com.paybank.generated.api.AccountsApi;
import com.paybank.generated.model.AccountResponse;
import com.paybank.generated.model.MovementPageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController implements AccountsApi {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public ResponseEntity<AccountResponse> getMyAccount() {
        return ResponseEntity.ok(accountService.getMyAccount());
    }

    @Override
    public ResponseEntity<MovementPageResponse> getMyMovements(Integer page, Integer size) {
        return ResponseEntity.ok(accountService.getMyMovements(PageQuery.of(page, size, null)));
    }
}
