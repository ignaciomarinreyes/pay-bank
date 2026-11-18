package com.paybank.security;

import com.paybank.config.JwtConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Builds the HttpOnly cookie that carries the refresh token. */
@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    private final JwtConfig jwtConfig;
    private final boolean secure;

    public RefreshCookieFactory(JwtConfig jwtConfig, @Value("${auth.refresh-cookie.secure:true}") boolean secure) {
        this.jwtConfig = jwtConfig;
        this.secure = secure;
    }

    public ResponseCookie create(String value) {
        return base(value).maxAge(jwtConfig.refreshTokenTtl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);
    }
}
