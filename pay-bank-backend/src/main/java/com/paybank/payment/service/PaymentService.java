package com.paybank.payment.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import com.paybank.account.exception.AccountNotFoundException;
import com.paybank.account.model.Account;
import com.paybank.account.model.Movement;
import com.paybank.account.model.MovementType;
import com.paybank.account.repository.AccountRepository;
import com.paybank.account.repository.MovementRepository;
import com.paybank.payment.exception.InsufficientBalanceException;
import com.paybank.payment.exception.InvalidPaymentAmountException;
import com.paybank.payment.exception.RecipientNotFoundException;
import com.paybank.payment.exception.SelfPaymentException;
import com.paybank.payment.model.Payment;
import com.paybank.payment.repository.PaymentRepository;
import com.paybank.security.CurrentUserProvider;
import com.paybank.user.model.User;
import com.paybank.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    /** Outcome of a payment, built inside the transaction so it can be read without a session. */
    public record Result(Long id, String recipientAlias, BigDecimal amount, String concept, Instant createdAt,
            BigDecimal balanceAfter) {
    }

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final MovementRepository movementRepository;
    private final Clock clock;

    public PaymentService(CurrentUserProvider currentUserProvider, UserRepository userRepository,
            AccountRepository accountRepository, PaymentRepository paymentRepository,
            MovementRepository movementRepository, Clock clock) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.movementRepository = movementRepository;
        this.clock = clock;
    }

    /**
     * Pays {@code amount} from the account of the authenticated user to the account of the user with
     * the given alias. Everything (payment, balances and both movements) is done in one transaction.
     */
    @Transactional
    public Result createPayment(String recipientAlias, BigDecimal amount, String concept) {
        BigDecimal value = validAmount(amount);
        String cleanConcept = cleanConcept(concept);

        User payer = userRepository.findById(currentUserProvider.currentUser().id())
                .orElseThrow(AccountNotFoundException::new);
        Long originId = accountRepository.findIdByUserId(payer.getId()).orElseThrow(AccountNotFoundException::new);

        User recipient = userRepository.findByAlias(User.normalizeAlias(recipientAlias))
                .orElseThrow(RecipientNotFoundException::new);
        Long destinationId = accountRepository.findIdByUserId(recipient.getId())
                .orElseThrow(RecipientNotFoundException::new);
        if (originId.equals(destinationId)) {
            throw new SelfPaymentException();
        }

        // Lock both accounts always in ascending id order, so crossed payments cannot deadlock
        Account origin;
        Account destination;
        if (originId < destinationId) {
            origin = lock(originId);
            destination = lock(destinationId);
        } else {
            destination = lock(destinationId);
            origin = lock(originId);
        }

        if (origin.getBalance().compareTo(value) < 0) {
            throw new InsufficientBalanceException();
        }
        origin.debit(value);
        destination.credit(value);

        Instant now = clock.instant();
        Payment payment = paymentRepository.save(new Payment(origin, destination, value, cleanConcept, now));
        movementRepository.save(new Movement(origin, payment, MovementType.CHARGE, value, origin.getBalance(), now));
        movementRepository.save(
                new Movement(destination, payment, MovementType.CREDIT, value, destination.getBalance(), now));

        return new Result(payment.getId(), recipient.getAlias(), value, cleanConcept, now, origin.getBalance());
    }

    private Account lock(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId).orElseThrow(AccountNotFoundException::new);
    }

    /** Greater than 0 and with at most 2 decimals; returned with scale 2. */
    private static BigDecimal validAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidPaymentAmountException();
        }
        return amount.setScale(2);
    }

    /** A blank concept is the same as no concept. */
    private static String cleanConcept(String concept) {
        if (concept == null) {
            return null;
        }
        String trimmed = concept.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
