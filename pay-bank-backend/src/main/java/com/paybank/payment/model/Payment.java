package com.paybank.payment.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.paybank.account.model.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_account_id", nullable = false)
    private Account originAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column
    private String concept;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public Payment(Account originAccount, Account destinationAccount, BigDecimal amount, String concept,
            Instant createdAt) {
        this.originAccount = originAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.concept = concept;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Account getOriginAccount() {
        return originAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getConcept() {
        return concept;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
