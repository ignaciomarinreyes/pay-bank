package com.paybank.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

import com.paybank.config.JwtConfig;
import com.paybank.user.model.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String ALIAS_CLAIM = "alias";

    private final JwtConfig config;
    private final Clock clock;
    private final SecretKey key;

    public JwtTokenProvider(JwtConfig config, Clock clock) {
        this.config = config;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(config.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(ALIAS_CLAIM, user.getAlias())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(config.accessTokenTtl())))
                .signWith(key)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return config.accessTokenTtl().toSeconds();
    }

    /**
     * Validates signature and expiration and returns the authenticated user.
     *
     * @throws JwtException if the token is malformed, tampered with or expired
     */
    public AuthenticatedUser parse(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AuthenticatedUser(Long.valueOf(claims.getSubject()), claims.get(ALIAS_CLAIM, String.class));
        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid token", e);
        }
    }
}
