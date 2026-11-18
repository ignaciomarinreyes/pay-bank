package com.paybank.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTests {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @CsvSource({
            "recipient, 422, RECIPIENT_NOT_FOUND",
            "self, 422, SELF_PAYMENT",
            "balance, 422, INSUFFICIENT_BALANCE",
            "account, 422, ACCOUNT_NOT_FOUND",
            "amount, 400, VALIDATION_ERROR"})
    void businessExceptionsAreAnsweredWithTheirStatusAndCode(String type, int status, String code) throws Exception {
        mvc.perform(get("/api/test/errors/" + type).with(user("test")))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value(status));
    }

    @Test
    void invalidPaginationParametersCarryTheValidationCode() throws Exception {
        mvc.perform(get("/api/accounts/me/movements?size=101").with(user("test")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
