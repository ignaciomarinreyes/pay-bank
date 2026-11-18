package com.paybank.user.model;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Always stored lowercase (the alias is case-insensitive). */
    @Column(nullable = false, unique = true)
    private String alias;

    /** BCrypt hash. */
    @Column(nullable = false)
    private String password;

    protected User() {
    }

    public User(String alias, String password) {
        this.alias = normalizeAlias(alias);
        this.password = password;
    }

    public static String normalizeAlias(String alias) {
        return alias == null ? null : alias.toLowerCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public String getPassword() {
        return password;
    }
}
