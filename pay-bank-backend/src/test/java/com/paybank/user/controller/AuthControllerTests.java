package com.paybank.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import com.paybank.support.MutableClock;
import com.paybank.support.TestClockConfig;
import com.paybank.user.model.User;
import com.paybank.user.repository.RefreshTokenRepository;
import com.paybank.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class AuthControllerTests {

    @Autowired
    MockMvc mvc;
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
        users.save(new User("maria", encoder.encode("Otra2")));
    }

    private MvcResult login(String alias, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"alias\":\"" + alias + "\",\"password\":\"" + password + "\"}")).andReturn();
    }

    private static String accessToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private static String refreshCookieValue(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).isNotNull();
        return header.substring("refresh_token=".length(), header.indexOf(';'));
    }

    private int ping(String token) throws Exception {
        var request = get("/api/test/ping");
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    // CA-03
    @Test
    void loginOpensSession() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"ana\",\"password\":\"Secreta1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("ana"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")));
    }

    // CA-04
    @Test
    void aliasIsCaseInsensitive() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"ANA\",\"password\":\"Secreta1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("ana"));
    }

    // CA-05, CA-06, CA-07
    @Test
    void wrongPasswordAndUnknownAliasGiveTheSameResponse() throws Exception {
        MvcResult wrongPassword = login("ana", "wrong");
        MvcResult unknownAlias = login("noexiste", "whatever");
        MvcResult wrongCase = login("ana", "secreta1");

        for (MvcResult r : new MvcResult[] {wrongPassword, unknownAlias, wrongCase}) {
            assertThat(r.getResponse().getStatus()).isEqualTo(401);
            assertThat(r.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        }
        assertThat(unknownAlias.getResponse().getContentAsString())
                .isEqualTo(wrongPassword.getResponse().getContentAsString());
        assertThat(tokens.count()).isZero();
    }

    @Test
    void emptyFieldsAreRejectedWithBadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // CA-09
    @Test
    void apiRequiresAValidAccessToken() throws Exception {
        String valid = accessToken(login("ana", "Secreta1"));

        assertThat(ping(null)).isEqualTo(401);
        assertThat(ping("garbage")).isEqualTo(401);
        assertThat(ping(valid.substring(0, valid.length() - 3) + "abc")).isEqualTo(401);
        assertThat(ping(valid)).isEqualTo(200);

        mvc.perform(get("/api/test/ping"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));

        clock.advance(Duration.ofMinutes(16));
        assertThat(ping(valid)).isEqualTo(401);
    }

    @Test
    void swaggerDocsAreAccessibleWithoutToken() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void corsPreflightAllowsFrontendOriginWithCredentials() throws Exception {
        mvc.perform(options("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    // CA-17
    @Test
    void anyAuthenticatedUserHasTheSameAccess() throws Exception {
        assertThat(ping(accessToken(login("ana", "Secreta1")))).isEqualTo(200);
        assertThat(ping(accessToken(login("maria", "Otra2")))).isEqualTo(200);
    }

    // CA-11
    @Test
    void refreshRenewsAnExpiredAccessToken() throws Exception {
        MvcResult login = login("ana", "Secreta1");
        String oldToken = accessToken(login);
        clock.advance(Duration.ofMinutes(16));
        assertThat(ping(oldToken)).isEqualTo(401);

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshCookieValue(login))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("ana"))
                .andReturn();

        assertThat(ping(accessToken(refreshed))).isEqualTo(200);
    }

    // CA-12
    @Test
    void refreshTokenCanOnlyBeUsedOnce() throws Exception {
        String cookie = refreshCookieValue(login("ana", "Secreta1"));

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", cookie)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", cookie)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void refreshWithoutCookieIsRejected() throws Exception {
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    // CA-13
    @Test
    void refreshTokenExpiresAfterSevenDays() throws Exception {
        String cookie = refreshCookieValue(login("ana", "Secreta1"));
        clock.advance(Duration.ofDays(7).plusSeconds(1));
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", cookie)))
                .andExpect(status().isUnauthorized());

        clock.reset();
        String second = refreshCookieValue(login("ana", "Secreta1"));
        clock.advance(Duration.ofDays(6));
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", second)))
                .andExpect(status().isOk());
    }

    // CA-14
    @Test
    void logoutInvalidatesTheRefreshToken() throws Exception {
        String cookie = refreshCookieValue(login("ana", "Secreta1"));

        mvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", cookie)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", cookie)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
    }
}
