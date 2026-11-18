package com.paybank.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.paybank.account.model.Account;
import com.paybank.user.model.User;
import com.paybank.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class AccountRepositoryTests {

    @Autowired
    UserRepository users;
    @Autowired
    AccountRepository accounts;
    @Autowired
    EntityManager entityManager;

    @Test
    void findIdByUserIdReturnsTheAccountIdOrNothing() {
        User ana = users.save(new User("ana", "hash"));
        User luis = users.save(new User("luis", "hash"));
        Account account = accounts.save(new Account(ana, new BigDecimal("100.00")));

        assertThat(accounts.findIdByUserId(ana.getId())).contains(account.getId());
        assertThat(accounts.findIdByUserId(luis.getId())).isEmpty();
    }

    @Test
    void debitAndCreditUpdateAndPersistTheBalance() {
        User ana = users.save(new User("ana", "hash"));
        Account account = accounts.save(new Account(ana, new BigDecimal("100.00")));

        Account locked = accounts.findByIdForUpdate(account.getId()).orElseThrow();
        locked.debit(new BigDecimal("30.50"));
        locked.credit(new BigDecimal("0.25"));
        accounts.flush();
        entityManager.clear();

        BigDecimal balance = accounts.findById(account.getId()).orElseThrow().getBalance();
        assertThat(balance).isEqualByComparingTo("69.75");
        assertThat(balance.scale()).isEqualTo(2);
    }

    @Test
    void findByIdForUpdateReturnsNothingForUnknownIds() {
        assertThat(accounts.findByIdForUpdate(-1L)).isEmpty();
    }
}
