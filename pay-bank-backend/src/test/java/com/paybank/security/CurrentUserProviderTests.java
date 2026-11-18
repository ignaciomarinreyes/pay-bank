package com.paybank.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserProviderTests {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheAuthenticatedUser() {
        var user = new AuthenticatedUser(7L, "ana");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertThat(provider.currentUser()).isEqualTo(user);
    }

    @Test
    void failsWithoutAuthentication() {
        assertThatThrownBy(provider::currentUser).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
