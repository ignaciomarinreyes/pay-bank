package com.paybank.support;

import java.math.BigDecimal;
import java.time.Instant;

import com.paybank.account.model.Account;
import com.paybank.account.model.Movement;
import com.paybank.account.model.MovementType;
import com.paybank.account.repository.AccountRepository;
import com.paybank.account.repository.MovementRepository;
import com.paybank.payment.model.Payment;
import com.paybank.payment.repository.PaymentRepository;
import com.paybank.user.model.User;
import com.paybank.user.repository.UserRepository;

/** Creates users, accounts, payments and their two movements with controlled values. */
public class TestDataFactory {

    private final UserRepository users;
    private final AccountRepository accounts;
    private final PaymentRepository payments;
    private final MovementRepository movements;

    public TestDataFactory(UserRepository users, AccountRepository accounts, PaymentRepository payments,
            MovementRepository movements) {
        this.users = users;
        this.accounts = accounts;
        this.payments = payments;
        this.movements = movements;
    }

    public Account account(String alias, String passwordHash, String balance) {
        User user = users.save(new User(alias, passwordHash));
        return accounts.save(new Account(user, new BigDecimal(balance)));
    }

    /** Creates the payment plus a charge in the origin account and a credit in the destination. */
    public Payment payment(Account origin, Account destination, String amount, String concept, Instant createdAt,
            String originBalanceAfter, String destinationBalanceAfter) {
        BigDecimal value = new BigDecimal(amount);
        Payment payment = payments.save(new Payment(origin, destination, value, concept, createdAt));
        movements.save(new Movement(origin, payment, MovementType.CHARGE, value,
                new BigDecimal(originBalanceAfter), createdAt));
        movements.save(new Movement(destination, payment, MovementType.CREDIT, value,
                new BigDecimal(destinationBalanceAfter), createdAt));
        return payment;
    }

    /** Deletes movements, payments and accounts (not users) in foreign key order. */
    public void deleteAllExceptUsers() {
        movements.deleteAll();
        payments.deleteAll();
        accounts.deleteAll();
    }
}
