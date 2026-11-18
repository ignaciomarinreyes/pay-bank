package com.paybank.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.paybank.support.MutableClock;
import com.paybank.support.TestClockConfig;
import com.paybank.user.exception.InvalidCredentialsException;
import com.paybank.user.exception.InvalidRefreshTokenException;
import com.paybank.user.model.User;
import com.paybank.user.repository.RefreshTokenRepository;
import com.paybank.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@Import(TestClockConfig.class)
class AuthServiceTests {

    @Autowired
    AuthService authService;
    @Autowired
    UserRepository users;
    @Autowired
    RefreshTokenRepository tokens;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.reset();
        tokens.deleteAll();
        users.deleteAll();
        users.save(new User("ana", encoder.encode("Secreta1")));
    }

    @Test
    void loginReturnsSessionAndStoresOnlyTheHash() {
        AuthService.Session session = authService.login("ANA", "Secreta1");

        assertThat(session.alias()).isEqualTo("ana");
        assertThat(session.expiresIn()).isEqualTo(900);
        assertThat(session.accessToken()).isNotBlank();
        assertThat(tokens.count()).isEqualTo(1);
        assertThat(tokens.findAll().getFirst().getTokenHash())
                .hasSize(64)
                .isNotEqualTo(session.refreshToken());
    }

    @Test
    void invalidCredentialsFailWithoutCreatingTokens() {
        assertThatThrownBy(() -> authService.login("ana", "secreta1")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login("noexiste", "x")).isInstanceOf(InvalidCredentialsException.class);
        assertThat(tokens.count()).isZero();
    }

    @Test
    void refreshRotatesTheToken() {
        AuthService.Session first = authService.login("ana", "Secreta1");

        AuthService.Session second = authService.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(tokens.count()).isEqualTo(2);
        assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(authService.refresh(second.refreshToken()).alias()).isEqualTo("ana");
    }

    @Test
    void refreshRejectsMissingUnknownAndExpiredTokens() {
        assertThatThrownBy(() -> authService.refresh(null)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh("unknown")).isInstanceOf(InvalidRefreshTokenException.class);

        AuthService.Session session = authService.login("ana", "Secreta1");
        clock.advance(Duration.ofDays(7).plusSeconds(1));
        assertThatThrownBy(() -> authService.refresh(session.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logoutRevokesTheTokenAndIsIdempotent() {
        AuthService.Session session = authService.login("ana", "Secreta1");

        authService.logout(session.refreshToken());
        authService.logout(session.refreshToken());
        authService.logout(null);
        authService.logout("unknown");

        assertThatThrownBy(() -> authService.refresh(session.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void concurrentRefreshesWithTheSameTokenSucceedOnlyOnce() throws Exception {
        String refreshToken = authService.login("ana", "Secreta1").refreshToken();
        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<Boolean> task = () -> {
                start.await();
                try {
                    authService.refresh(refreshToken);
                    return true;
                } catch (InvalidRefreshTokenException e) {
                    return false;
                }
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        int successes = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                successes++;
            }
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
    }
}
