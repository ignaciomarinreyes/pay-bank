package com.paybank.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.paybank.account.exception.AccountNotFoundException;
import com.paybank.account.model.Account;
import com.paybank.account.model.MovementType;
import com.paybank.account.repository.AccountRepository;
import com.paybank.account.repository.MovementRepository;
import com.paybank.payment.exception.InsufficientBalanceException;
import com.paybank.payment.exception.InvalidPaymentAmountException;
import com.paybank.payment.exception.RecipientNotFoundException;
import com.paybank.payment.exception.SelfPaymentException;
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

@SpringBootTest
class PaymentServiceTests {

    @Autowired
    PaymentService paymentService;
    @Autowired
    UserRepository users;
    @Autowired
    AccountRepository accounts;
    @Autowired
    PaymentRepository payments;
    @Autowired
    MovementRepository movements;
    @Autowired
    RefreshTokenRepository tokens;

    TestDataFactory data;
    Account ana;
    Account maria;

    @BeforeEach
    void setUp() {
        clean();
        data = new TestDataFactory(users, accounts, payments, movements);
        ana = data.account("ana", "hash", "100.00");
        maria = data.account("maria", "hash", "500.00");
        loginAs(ana);
    }

    @AfterEach
    void clean() {
        SecurityContextHolder.clearContext();
        movements.deleteAll();
        payments.deleteAll();
        accounts.deleteAll();
        tokens.deleteAll();
        users.deleteAll();
    }

    private static void loginAs(Account account) {
        var user = new AuthenticatedUser(account.getUser().getId(), account.getUser().getAlias());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private BigDecimal balanceOf(Account account) {
        return accounts.findById(account.getId()).orElseThrow().getBalance();
    }

    // CA-07
    @Test
    void unknownRecipientIsRejectedAndNothingChanges() {
        assertThatThrownBy(() -> paymentService.createPayment("noexiste", new BigDecimal("10"), null))
                .isInstanceOf(RecipientNotFoundException.class);

        assertThat(balanceOf(ana)).isEqualByComparingTo("100.00");
        assertThat(payments.count()).isZero();
    }

    @Test
    void aRecipientWithoutAccountIsTreatedAsUnknown() {
        users.save(new com.paybank.user.model.User("luis", "hash"));

        assertThatThrownBy(() -> paymentService.createPayment("luis", new BigDecimal("10"), null))
                .isInstanceOf(RecipientNotFoundException.class);
    }

    @Test
    void theUserMustHaveAnAccount() {
        var luis = users.save(new com.paybank.user.model.User("luis", "hash"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(luis.getId(), "luis"), null, List.of()));

        assertThatThrownBy(() -> paymentService.createPayment("maria", new BigDecimal("10"), null))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // CA-08
    @Test
    void payingYourselfIsRejectedWhateverTheCase() {
        assertThatThrownBy(() -> paymentService.createPayment("ANA", new BigDecimal("10"), null))
                .isInstanceOf(SelfPaymentException.class);
    }

    // CA-09
    @Test
    void insufficientBalanceIsRejectedAndNothingChanges() {
        assertThatThrownBy(() -> paymentService.createPayment("maria", new BigDecimal("100.01"), null))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(balanceOf(ana)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(maria)).isEqualByComparingTo("500.00");
        assertThat(movements.count()).isZero();
    }

    // CA-10
    @Test
    void thePaymentMayLeaveTheBalanceAtZero() {
        var result = paymentService.createPayment("maria", new BigDecimal("100.00"), null);

        assertThat(result.balanceAfter()).isEqualByComparingTo("0.00");
        assertThat(balanceOf(ana)).isEqualByComparingTo("0.00");
        assertThat(balanceOf(maria)).isEqualByComparingTo("600.00");
    }

    @Test
    void invalidAmountsAreRejected() {
        for (String amount : new String[] {"0", "-5", "10.999", "0.001"}) {
            assertThatThrownBy(() -> paymentService.createPayment("maria", new BigDecimal(amount), null))
                    .isInstanceOf(InvalidPaymentAmountException.class);
        }
        assertThatThrownBy(() -> paymentService.createPayment("maria", null, null))
                .isInstanceOf(InvalidPaymentAmountException.class);
        assertThat(payments.count()).isZero();
    }

    @Test
    void amountsWithTrailingZerosAreAcceptedAndStoredWithScaleTwo() {
        var result = paymentService.createPayment("maria", new BigDecimal("10.500"), null);

        assertThat(result.amount()).isEqualByComparingTo("10.50");
        assertThat(result.amount().scale()).isEqualTo(2);
    }

    // CA-11, CA-12
    @Test
    void createsThePaymentAndBothMovementsWithTheResultingBalances() {
        var result = paymentService.createPayment("Maria", new BigDecimal("25.50"), "  Cena  ");

        assertThat(result.recipientAlias()).isEqualTo("maria");
        assertThat(result.concept()).isEqualTo("Cena");
        assertThat(result.balanceAfter()).isEqualByComparingTo("74.50");
        assertThat(balanceOf(ana)).isEqualByComparingTo("74.50");
        assertThat(balanceOf(maria)).isEqualByComparingTo("525.50");

        var rows = movements.findAll();
        assertThat(rows).hasSize(2);
        var charge = rows.stream().filter(m -> m.getType() == MovementType.CHARGE).findFirst().orElseThrow();
        var credit = rows.stream().filter(m -> m.getType() == MovementType.CREDIT).findFirst().orElseThrow();
        assertThat(charge.getBalanceAfter()).isEqualByComparingTo("74.50");
        assertThat(credit.getBalanceAfter()).isEqualByComparingTo("525.50");
        assertThat(charge.getCreatedAt()).isEqualTo(credit.getCreatedAt());
        assertThat(charge.getCreatedAt()).isCloseTo(result.createdAt(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    void aBlankConceptIsStoredAsNoConcept() {
        assertThat(paymentService.createPayment("maria", new BigDecimal("1"), "   ").concept()).isNull();
        assertThat(paymentService.createPayment("maria", new BigDecimal("1"), "").concept()).isNull();
        assertThat(payments.findAll()).allMatch(p -> p.getConcept() == null);
    }

    // CA-16
    @Test
    void twoSimultaneousPaymentsCannotLeaveTheBalanceNegative() throws Exception {
        List<Boolean> outcomes = runConcurrently(List.of(
                () -> pay(ana, "maria", "60.00"),
                () -> pay(ana, "maria", "60.00")));

        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(balanceOf(ana)).isEqualByComparingTo("40.00");
        assertThat(balanceOf(maria)).isEqualByComparingTo("560.00");
        assertThat(movements.count()).isEqualTo(2);
    }

    @Test
    void crossedSimultaneousPaymentsFinishWithoutDeadlock() throws Exception {
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> pay(ana, "maria", "1.00"));
            tasks.add(() -> pay(maria, "ana", "2.00"));
        }

        List<Boolean> outcomes = runConcurrently(tasks);

        assertThat(outcomes).containsOnly(true);
        assertThat(balanceOf(ana)).isEqualByComparingTo("104.00");
        assertThat(balanceOf(maria)).isEqualByComparingTo("496.00");
        assertThat(movements.count()).isEqualTo(16);
    }

    private boolean pay(Account payer, String recipient, String amount) {
        loginAs(payer);
        try {
            paymentService.createPayment(recipient, new BigDecimal(amount), null);
            return true;
        } catch (InsufficientBalanceException e) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<Boolean> runConcurrently(List<Callable<Boolean>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        List<Boolean> results = new ArrayList<>();
        for (Future<Boolean> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return results;
    }
}
