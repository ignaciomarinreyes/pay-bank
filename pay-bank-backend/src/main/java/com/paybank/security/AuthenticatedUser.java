package com.paybank.security;

/** Principal built from a valid access token. */
public record AuthenticatedUser(Long id, String alias) {
}
