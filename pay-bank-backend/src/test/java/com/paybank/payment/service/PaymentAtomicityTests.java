package com.paybank.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.math.BigDecimal;
import java.util.List;

import com.paybank.account.model.Account;
import com.paybank.account.model.Movement;
import com.paybank.account.model.MovementType;
import com.paybank.account.repository.AccountRepository;
import com.paybank.account.repository.MovementRepository;
import com.paybank.payment.repository.PaymentRepository;
import com.paybank.security.AuthenticatedUser;
import com.paybank.support.TestDataFactory;
import com.paybank.user.repository.RefreshTokenRepository;
import com.paybank.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class PaymentAtomicityTests {

    @Autowired
    PaymentService paymentService;
    @Autowired
    UserRepository users;
    @Autowired
    AccountRepository accounts;
    @Autowired
    PaymentRepository payments;
    @MockitoSpyBean
    MovementRepository movements;
    @Autowired
    RefreshTokenRepository tokens;

    Account ana;
    Account maria;

    @BeforeEach
    void setUp() {
        clean();
        var data = new TestDataFactory(users, accounts, payments, movements);
        ana = data.account("ana", "hash", "100.00");
        maria = data.account("maria", "hash", "500.00");
        var user = new AuthenticatedUser(ana.getUser().getId(), "ana");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void clean() {
        reset(movements);
        SecurityContextHolder.clearContext();
        movements.deleteAll();
        payments.deleteAll();
        accounts.deleteAll();
        tokens.deleteAll();
        users.deleteAll();
    }

    // CA-15
    @Test
    void aFailureWhileSavingTheSecondMovementRollsEverythingBack() {
        doThrow(new IllegalStateException("boom")).when(movements)
                .save(argThat((Movement m) -> m != null && m.getType() == MovementType.CREDIT));

        assertThatThrownBy(() -> paymentService.createPayment("maria", new BigDecimal("30.00"), "x"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(accounts.findById(ana.getId()).orElseThrow().getBalance()).isEqualByComparingTo("100.00");
        assertThat(accounts.findById(maria.getId()).orElseThrow().getBalance()).isEqualByComparingTo("500.00");
        assertThat(payments.count()).isZero();
        assertThat(movements.count()).isZero();
    }
}
