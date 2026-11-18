package com.paybank.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTests {

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

    Account ana;
    Account maria;
    Account pobre;

    @BeforeEach
    void setUp() {
        clean();
        var data = new TestDataFactory(users, accounts, payments, movements);
        ana = data.account("ana", encoder.encode("ana-pass"), "1000.00");
        maria = data.account("maria", encoder.encode("maria-pass"), "500.00");
        pobre = data.account("pobre", encoder.encode("pobre-pass"), "100.00");
    }

    @AfterEach
    void clean() {
        movements.deleteAll();
        payments.deleteAll();
        accounts.deleteAll();
        tokens.deleteAll();
        users.deleteAll();
    }

    private String token(String alias) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alias\":\"" + alias + "\",\"password\":\"" + alias + "-pass\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    private ResultActions pay(String payer, String json) throws Exception {
        return mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(payer)).content(json));
    }

    private ResultActions pay(String payer, String recipient, String amount, String concept) throws Exception {
        String conceptJson = concept == null ? "null" : "\"" + concept + "\"";
        return pay(payer, "{\"recipientAlias\":\"" + recipient + "\",\"amount\":" + amount + ",\"concept\":" + conceptJson + "}");
    }

    private BigDecimal balanceOf(Account account) {
        return accounts.findById(account.getId()).orElseThrow().getBalance();
    }

    // CA-04
    @Test
    void invalidAmountsAreRejectedWithAValidationError() throws Exception {
        for (String amount : new String[] {"0", "-5", "10.999"}) {
            pay("ana", "maria", amount, null)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        assertThat(payments.count()).isZero();
    }

    @Test
    void missingOrEmptyFieldsAreRejectedWithAValidationError() throws Exception {
        pay("ana", "{\"amount\":10}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        pay("ana", "{\"recipientAlias\":\"\",\"amount\":10}").andExpect(status().isBadRequest());
        pay("ana", "{\"recipientAlias\":\"maria\"}").andExpect(status().isBadRequest());
        pay("ana", "{\"recipientAlias\":\"maria\",\"amount\":\"abc\"}").andExpect(status().isBadRequest());
        assertThat(payments.count()).isZero();
    }

    // CA-05
    @Test
    void anAmountWithTwoDecimalsIsPaidExactly() throws Exception {
        pay("ana", "maria", "12.50", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(12.50))
                .andExpect(jsonPath("$.recipientAlias").value("maria"))
                .andExpect(jsonPath("$.balanceAfter").value(987.50));
        assertThat(balanceOf(ana)).isEqualByComparingTo("987.50");
    }

    // CA-06
    @Test
    void theConceptMayHaveAtMost140Characters() throws Exception {
        pay("ana", "maria", "1", "x".repeat(141))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        pay("ana", "maria", "1", "x".repeat(140)).andExpect(status().isCreated());
        assertThat(payments.count()).isEqualTo(1);
    }

    // CA-07
    @Test
    void anUnknownRecipientIsRejected() throws Exception {
        pay("ana", "noexiste", "10", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RECIPIENT_NOT_FOUND"));
        assertThat(balanceOf(ana)).isEqualByComparingTo("1000.00");
        assertThat(payments.count()).isZero();
    }

    // CA-08
    @Test
    void payingYourselfIsRejected() throws Exception {
        pay("ana", "ANA", "10", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SELF_PAYMENT"));
        assertThat(payments.count()).isZero();
    }

    // CA-09
    @Test
    void anAmountOverTheBalanceIsRejected() throws Exception {
        pay("pobre", "maria", "100.01", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
        assertThat(balanceOf(pobre)).isEqualByComparingTo("100.00");
        assertThat(payments.count()).isZero();
    }

    // CA-10
    @Test
    void theWholeBalanceCanBePaid() throws Exception {
        pay("pobre", "maria", "100.00", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(0.00));
        assertThat(balanceOf(pobre)).isEqualByComparingTo("0.00");
    }

    // CA-11
    @Test
    void thePaymentUpdatesBothBalancesAndShowsUpInTheMovementsOfBothUsers() throws Exception {
        pay("ana", "Maria", "250.00", "Cena")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.concept").value("Cena"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(balanceOf(ana)).isEqualByComparingTo("750.00");
        assertThat(balanceOf(maria)).isEqualByComparingTo("750.00");
        mvc.perform(get("/api/accounts/me/movements").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ana")))
                .andExpect(jsonPath("$.content[0].type").value("CHARGE"))
                .andExpect(jsonPath("$.content[0].amount").value(-250.00))
                .andExpect(jsonPath("$.content[0].counterpartyAlias").value("maria"))
                .andExpect(jsonPath("$.content[0].concept").value("Cena"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(750.00));
        mvc.perform(get("/api/accounts/me/movements").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("maria")))
                .andExpect(jsonPath("$.content[0].type").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].amount").value(250.00))
                .andExpect(jsonPath("$.content[0].counterpartyAlias").value("ana"))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(750.00));
    }

    // CA-12
    @Test
    void aBlankOrMissingConceptIsStoredAsNull() throws Exception {
        pay("ana", "maria", "1", "   ").andExpect(status().isCreated()).andExpect(jsonPath("$.concept").isEmpty());
        pay("ana", "{\"recipientAlias\":\"maria\",\"amount\":1}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.concept").isEmpty());

        mvc.perform(get("/api/accounts/me/movements").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ana")))
                .andExpect(jsonPath("$.content[0].concept").isEmpty())
                .andExpect(jsonPath("$.content[1].concept").isEmpty());
        assertThat(payments.findAll()).allMatch(p -> p.getConcept() == null);
    }

    // CA-17
    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientAlias\":\"maria\",\"amount\":10}"))
                .andExpect(status().isUnauthorized());
        assertThat(payments.count()).isZero();
        assertThat(balanceOf(maria)).isEqualByComparingTo("500.00");
    }
}
