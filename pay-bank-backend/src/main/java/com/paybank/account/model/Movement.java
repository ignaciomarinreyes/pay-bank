package com.paybank.account.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.paybank.payment.model.Payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "movements", indexes = @Index(name = "idx_movements_account_created", columnList = "account_id, created_at, id"))
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    /** Always positive; the direction is given by {@link #type}. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Account balance right after applying this movement. */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Movement() {
    }

    public Movement(Account account, Payment payment, MovementType type, BigDecimal amount,
            BigDecimal balanceAfter, Instant createdAt) {
        this.account = account;
        this.payment = payment;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Payment getPayment() {
        return payment;
    }

    public MovementType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
