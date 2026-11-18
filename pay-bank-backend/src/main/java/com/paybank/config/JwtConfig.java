package com.paybank.config;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings: signing secret and token lifetimes.
 */
@Validated
@ConfigurationProperties("jwt")
public record JwtConfig(
        @NotNull @Size(min = 32) String secret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("7d") Duration refreshTokenTtl) {
}
