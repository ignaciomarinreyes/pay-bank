package com.paybank.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.paybank.user.model.RefreshToken;
import com.paybank.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserRepositoriesTests {

    @Autowired
    UserRepository users;

    @Autowired
    RefreshTokenRepository tokens;

    @Test
    void duplicateAliasViolatesUniqueConstraint() {
        users.saveAndFlush(new User("ana", "hash"));
        assertThatThrownBy(() -> users.saveAndFlush(new User("ANA", "other")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByAliasFindsSavedUser() {
        users.saveAndFlush(new User("Ana", "hash"));
        assertThat(users.findByAlias("ana")).isPresent();
        assertThat(users.findByAlias("nobody")).isEmpty();
    }

    @Test
    void conditionalRevocationAffectsOneRowOnlyOnce() {
        User user = users.saveAndFlush(new User("ana", "hash"));
        Instant now = Instant.now();
        RefreshToken token = tokens.saveAndFlush(new RefreshToken(user, "h".repeat(64), now, now.plusSeconds(60)));

        assertThat(tokens.revoke(token.getId(), now)).isEqualTo(1);
        assertThat(tokens.revoke(token.getId(), now)).isEqualTo(0);
        assertThat(tokens.findByTokenHash("h".repeat(64)).orElseThrow().getRevokedAt()).isNotNull();
    }
}
