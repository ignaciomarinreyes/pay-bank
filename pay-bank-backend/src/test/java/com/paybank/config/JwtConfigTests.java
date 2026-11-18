package com.paybank.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JwtConfigTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @EnableConfigurationProperties(JwtConfig.class)
    static class Config {
    }

    @Test
    void shortSecretFailsContext() {
        runner.withPropertyValues("jwt.secret=too-short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void validSecretUsesDefaultDurations() {
        runner.withPropertyValues("jwt.secret=0123456789012345678901234567890123")
                .run(context -> {
                    JwtConfig config = context.getBean(JwtConfig.class);
                    assertThat(config.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
                    assertThat(config.refreshTokenTtl()).isEqualTo(Duration.ofDays(7));
                });
    }
}
