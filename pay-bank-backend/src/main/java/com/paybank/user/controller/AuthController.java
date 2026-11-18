package com.paybank.user.controller;

import com.paybank.generated.api.AuthApi;
import com.paybank.generated.model.AuthResponse;
import com.paybank.generated.model.LoginRequest;
import com.paybank.security.RefreshCookieFactory;
import com.paybank.user.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final RefreshCookieFactory cookies;

    public AuthController(AuthService authService, RefreshCookieFactory cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
        return toResponse(authService.login(loginRequest.getAlias(), loginRequest.getPassword()));
    }

    @Override
    public ResponseEntity<AuthResponse> refresh(String refreshToken) {
        return toResponse(authService.refresh(refreshToken));
    }

    @Override
    public ResponseEntity<Void> logout(String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> toResponse(AuthService.Session session) {
        AuthResponse body = new AuthResponse(session.accessToken(), "Bearer", session.expiresIn(), session.alias());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(session.refreshToken()).toString())
                .body(body);
    }
}
