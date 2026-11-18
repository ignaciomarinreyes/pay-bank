package com.paybank.account.model;

import java.math.BigDecimal;

import com.paybank.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Current balance: initial balance plus the sum of the account movements. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    protected Account() {
    }

    public Account(User user, BigDecimal balance) {
        this.user = user;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
