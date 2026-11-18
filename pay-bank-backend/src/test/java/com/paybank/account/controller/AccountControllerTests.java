package com.paybank.account.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.paybank.account.model.Account;
import com.paybank.account.repository.AccountRepository;
import com.paybank.account.repository.MovementRepository;
import com.paybank.payment.repository.PaymentRepository;
import com.paybank.support.TestDataFactory;
import com.paybank.user.repository.RefreshTokenRepository;
import com.paybank.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTests {

    private static final Instant BASE = Instant.parse("2026-11-01T10:00:00Z");
    private static final String URL = "/api/accounts/me/movements";

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    AccountRepository accounts;
    @Autowired
    PaymentRepository payments;
    @Autowired
    MovementRepository movements;
    @Autowired
    RefreshTokenRepository tokens;
    @Autowired
    PasswordEncoder encoder;

    TestDataFactory data;
    Account ana;
    Account maria;
    Account luis;

    @BeforeEach
    void setUp() {
        clean();
        data = new TestDataFactory(users, accounts, payments, movements);
        ana = data.account("ana", encoder.encode("ana-pass"), "1000.00");
        maria = data.account("maria", encoder.encode("maria-pass"), "1000.00");
        luis = data.account("luis", encoder.encode("luis-pass"), "1000.00");
    }

    @AfterEach
    void clean() {
        movements.deleteAll();
        payments.deleteAll();
        accounts.deleteAll();
        tokens.deleteAll();
        users.deleteAll();
    }

    private String token(String alias, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"" + alias + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder as(String alias, String uri)
            throws Exception {
        return get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token(alias, alias + "-pass"));
    }

    // CA-02
    @Test
    void accountEndpointReturnsTheBalanceOfTheCurrentUser() throws Exception {
        mvc.perform(as("ana", "/api/accounts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00));
        mvc.perform(get("/api/accounts/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void accountEndpointFailsForAUserWithoutAccount() throws Exception {
        accounts.deleteById(luis.getId());

        mvc.perform(as("luis", "/api/accounts/me"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    // CA-14
    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    // CA-02
    @Test
    void doesNotShowPaymentsOfOtherUsers() throws Exception {
        data.payment(maria, luis, "20.00", "not for ana", BASE, "980.00", "1020.00");

        mvc.perform(as("ana", URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(as("luis", URL))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].counterpartyAlias").value("maria"));
    }

    // CA-04, CA-05
    @Test
    void paginatesTwentyPerPage() throws Exception {
        for (int i = 0; i < 45; i++) {
            data.payment(ana, maria, "1.00", "p" + i, BASE.plusSeconds(60L * i), "100.00", "100.00");
        }

        mvc.perform(as("ana", URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(45))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].concept").value("p44"));
        mvc.perform(as("ana", URL + "?page=2"))
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[4].concept").value("p0"))
                .andExpect(jsonPath("$.page").value(2));
    }

    // CA-07
    @Test
    void chargeHasNegativeAmountAndRecipientAsCounterparty() throws Exception {
        data.payment(ana, maria, "1234.56", "Alquiler", BASE, "-234.56", "2234.56");

        mvc.perform(as("ana", URL))
                .andExpect(jsonPath("$.content[0].type").value("CHARGE"))
                .andExpect(jsonPath("$.content[0].amount").value(-1234.56))
                .andExpect(jsonPath("$.content[0].counterpartyAlias").value("maria"))
                .andExpect(jsonPath("$.content[0].concept").value("Alquiler"))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-11-01T10:00:00Z"));
    }

    // CA-08
    @Test
    void creditHasPositiveAmountAndPayerAsCounterparty() throws Exception {
        data.payment(maria, ana, "50.00", "x", BASE, "950.00", "1050.00");

        mvc.perform(as("ana", URL))
                .andExpect(jsonPath("$.content[0].type").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].amount").value(50.00))
                .andExpect(jsonPath("$.content[0].counterpartyAlias").value("maria"));
    }

    // CA-09
    @Test
    void returnsTheBalanceAfterEachMovement() throws Exception {
        data.payment(maria, ana, "50.00", "in", BASE, "950.00", "150.00");
        data.payment(ana, luis, "30.00", "out", BASE.plusSeconds(60), "120.00", "1030.00");

        mvc.perform(as("ana", URL))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(120.00))
                .andExpect(jsonPath("$.content[1].balanceAfter").value(150.00));
    }

    // CA-11
    @Test
    void paymentWithoutConceptHasNullConcept() throws Exception {
        data.payment(ana, maria, "5.00", null, BASE, "995.00", "1005.00");

        mvc.perform(as("ana", URL))
                .andExpect(jsonPath("$.content[0].concept").isEmpty())
                .andExpect(jsonPath("$.content[0].concept").hasJsonPath());
    }

    // CA-12
    @Test
    void usersWithoutMovementsOrWithoutAccountGetAnEmptyPage() throws Exception {
        mvc.perform(as("luis", URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        accounts.deleteById(luis.getId());
        mvc.perform(as("luis", URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsOutOfRangePaginationParameters() throws Exception {
        mvc.perform(as("ana", URL + "?size=101")).andExpect(status().isBadRequest());
        mvc.perform(as("ana", URL + "?size=0")).andExpect(status().isBadRequest());
        mvc.perform(as("ana", URL + "?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
