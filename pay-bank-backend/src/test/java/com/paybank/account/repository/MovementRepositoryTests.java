package com.paybank.account.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.paybank.account.model.Account;
import com.paybank.account.model.MovementType;
import com.paybank.payment.repository.PaymentRepository;
import com.paybank.support.TestDataFactory;
import com.paybank.user.model.User;
import com.paybank.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class MovementRepositoryTests {

    private static final Instant BASE = Instant.parse("2026-11-01T10:00:00Z");

    @Autowired
    UserRepository users;
    @Autowired
    AccountRepository accounts;
    @Autowired
    PaymentRepository payments;
    @Autowired
    MovementRepository movements;

    TestDataFactory data;
    Account ana;
    Account maria;
    Account luis;

    @BeforeEach
    void setUp() {
        data = new TestDataFactory(users, accounts, payments, movements);
        ana = data.account("ana", "hash", "1000.00");
        maria = data.account("maria", "hash", "1000.00");
        luis = data.account("luis", "hash", "1000.00");
    }

    private Page<MovementRow> page(Account account, int page, int size) {
        return movements.findPageByUserId(account.getUser().getId(), PageRequest.of(page, size));
    }

    @Test
    void persistsAccountPaymentAndMovementsWithScaleTwo() {
        data.payment(ana, maria, "1234.5", "Rent", BASE, "-234.50", "2234.50");

        MovementRow charge = page(ana, 0, 20).getContent().getFirst();
        assertThat(charge.type()).isEqualTo(MovementType.CHARGE);
        assertThat(charge.amount()).isEqualByComparingTo("1234.50");
        assertThat(charge.balanceAfter()).isEqualByComparingTo("-234.50");
        assertThat(accounts.findById(ana.getId()).orElseThrow().getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void aUserCannotHaveTwoAccounts() {
        User user = users.findByAlias("ana").orElseThrow();
        assertThatThrownBy(() -> accounts.saveAndFlush(new Account(user, BigDecimal.ZERO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onlyMovementsOfTheGivenUserAreReturned() {
        data.payment(ana, maria, "10.00", "a", BASE, "990.00", "1010.00");
        data.payment(maria, luis, "20.00", "b", BASE.plusSeconds(60), "990.00", "1020.00");

        assertThat(page(ana, 0, 20).getContent()).hasSize(1);
        assertThat(page(maria, 0, 20).getContent()).hasSize(2);
        assertThat(page(luis, 0, 20).getContent()).extracting(MovementRow::concept).containsExactly("b");
        assertThat(page(ana, 0, 20).getTotalElements()).isEqualTo(1);
    }

    @Test
    void movementsAreOrderedByDateDescendingThenIdDescending() {
        data.payment(ana, maria, "1.00", "oldest", BASE, "999.00", "1001.00");
        data.payment(ana, maria, "2.00", "newest", BASE.plus(2, ChronoUnit.DAYS), "997.00", "1003.00");
        data.payment(ana, maria, "3.00", "middle", BASE.plus(1, ChronoUnit.DAYS), "996.00", "1004.00");
        data.payment(ana, maria, "4.00", "tie-first-inserted", BASE.plus(3, ChronoUnit.DAYS), "992.00", "1008.00");
        data.payment(ana, maria, "5.00", "tie-last-inserted", BASE.plus(3, ChronoUnit.DAYS), "987.00", "1013.00");

        assertThat(page(ana, 0, 20).getContent()).extracting(MovementRow::concept)
                .containsExactly("tie-last-inserted", "tie-first-inserted", "newest", "middle", "oldest");
    }

    @Test
    void counterpartyIsTheRecipientForChargesAndThePayerForCredits() {
        data.payment(ana, maria, "10.00", "paid", BASE, "990.00", "1010.00");
        data.payment(maria, ana, "5.00", null, BASE.plusSeconds(60), "1005.00", "995.00");

        var rows = page(ana, 0, 20).getContent();
        assertThat(rows.get(0).type()).isEqualTo(MovementType.CREDIT);
        assertThat(rows.get(0).counterpartyAlias()).isEqualTo("maria");
        assertThat(rows.get(0).concept()).isNull();
        assertThat(rows.get(1).type()).isEqualTo(MovementType.CHARGE);
        assertThat(rows.get(1).counterpartyAlias()).isEqualTo("maria");
        assertThat(rows.get(1).concept()).isEqualTo("paid");
    }

    @Test
    void paginatesInTheDatabase() {
        for (int i = 0; i < 45; i++) {
            data.payment(ana, maria, "1.00", "p" + i, BASE.plusSeconds(60L * i), "100.00", "100.00");
        }

        Page<MovementRow> first = page(ana, 0, 20);
        assertThat(first.getContent()).hasSize(20);
        assertThat(first.getTotalElements()).isEqualTo(45);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(first.getContent().getFirst().concept()).isEqualTo("p44");

        Page<MovementRow> last = page(ana, 2, 20);
        assertThat(last.getContent()).hasSize(5);
        assertThat(last.getContent().getLast().concept()).isEqualTo("p0");
        assertThat(page(ana, 3, 20).getContent()).isEmpty();
    }

    @Test
    void aUserWithoutMovementsGetsAnEmptyPage() {
        Page<MovementRow> empty = page(luis, 0, 20);
        assertThat(empty.getContent()).isEmpty();
        assertThat(empty.getTotalElements()).isZero();
        assertThat(empty.getTotalPages()).isZero();
    }
}
