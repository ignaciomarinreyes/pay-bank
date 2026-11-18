package com.paybank.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.paybank.config.JwtConfig;
import com.paybank.support.MutableClock;
import com.paybank.user.model.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTests {

    private static final String SECRET = "test-secret-with-at-least-32-characters!!";

    private MutableClock clock;
    private JwtTokenProvider provider;
    private User user;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        provider = new JwtTokenProvider(
                new JwtConfig(SECRET, Duration.ofMinutes(15), Duration.ofDays(7)), clock);
        user = new User("ana", "hash");
        ReflectionTestUtils.setField(user, "id", 42L);
    }

    @Test
    void tokenContainsSubjectAndAlias() {
        AuthenticatedUser parsed = provider.parse(provider.generateAccessToken(user));
        assertThat(parsed.id()).isEqualTo(42L);
        assertThat(parsed.alias()).isEqualTo("ana");
    }

    @Test
    void tokenExpiresAfterFifteenMinutes() {
        String token = provider.generateAccessToken(user);

        clock.advance(Duration.ofMinutes(14).plusSeconds(59));
        assertThat(provider.parse(token).alias()).isEqualTo("ana");

        clock.advance(Duration.ofSeconds(2));
        assertThatThrownBy(() -> provider.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedAndMalformedTokens() {
        String token = provider.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThatThrownBy(() -> provider.parse(tampered)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> provider.parse("not-a-jwt")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> provider.parse("")).isInstanceOf(JwtException.class);
    }
}
