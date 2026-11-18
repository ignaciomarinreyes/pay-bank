package com.paybank.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import com.paybank.config.JwtConfig;
import com.paybank.security.JwtTokenProvider;
import com.paybank.user.exception.InvalidCredentialsException;
import com.paybank.user.exception.InvalidRefreshTokenException;
import com.paybank.user.model.RefreshToken;
import com.paybank.user.model.User;
import com.paybank.user.repository.RefreshTokenRepository;
import com.paybank.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /** Result of opening or renewing a session. */
    public record Session(String accessToken, long expiresIn, String alias, String refreshToken) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final JwtConfig jwtConfig;
    private final Clock clock;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository, JwtTokenProvider tokenProvider, JwtConfig jwtConfig,
            Clock clock) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.jwtConfig = jwtConfig;
        this.clock = clock;
    }

    @Transactional
    public Session login(String alias, String password) {
        String normalized = User.normalizeAlias(alias);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalized, password));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByAlias(normalized).orElseThrow(InvalidCredentialsException::new);
        return issueSession(user);
    }

    @Transactional
    public Session refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        Instant now = clock.instant();
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }
        if (refreshTokenRepository.revoke(token.getId(), now) != 1) {
            throw new InvalidRefreshTokenException();
        }
        return issueSession(token.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> refreshTokenRepository.revoke(token.getId(), clock.instant()));
    }

    private Session issueSession(User user) {
        Instant now = clock.instant();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(
                new RefreshToken(user, hash(rawRefresh), now, now.plus(jwtConfig.refreshTokenTtl())));
        return new Session(tokenProvider.generateAccessToken(user), tokenProvider.accessTokenTtlSeconds(),
                user.getAlias(), rawRefresh);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
