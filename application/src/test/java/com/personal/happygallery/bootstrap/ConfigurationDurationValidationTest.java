package com.personal.happygallery.bootstrap;

import com.personal.happygallery.adapter.out.external.notification.EmailVerificationProperties;
import com.personal.happygallery.adapter.out.external.payment.ExternalPaymentProperties;
import com.personal.happygallery.adapter.out.external.resilience.ExternalCircuitBreakerProperties;
import com.personal.happygallery.application.token.GuestTokenProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationDurationValidationTest {

    private static final String HMAC_SECRET = "s".repeat(32);

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DurationPropertiesConfig.class)
            .withPropertyValues("app.guest-token.hmac-secret=" + HMAC_SECRET);

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @DisplayName("외부 호출 Duration은 기존 밀리초 최소 단위를 유지한다")
    @Test
    void externalTimeout_preservesMillisecondMinimum() {
        var belowMinimum = new EmailVerificationProperties(
                null, "이메일 인증번호", Duration.ofNanos(999_999));
        var atMinimum = new EmailVerificationProperties(
                null, "이메일 인증번호", Duration.ofMillis(1));

        assertThat(validator.validate(belowMinimum))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("timeout");
        assertThat(validator.validate(atMinimum)).isEmpty();
    }

    @DisplayName("CircuitBreaker Duration은 기존 초 최소 단위를 유지한다")
    @Test
    void circuitBreaker_preservesSecondMinimum() {
        var threadPool = new ExternalPaymentProperties.ThreadPool(1, 1);
        var belowMinimum = new ExternalPaymentProperties(
                Duration.ofMillis(1),
                threadPool,
                new ExternalCircuitBreakerProperties(
                        50, 20, 10, Duration.ofMillis(999), 1));
        var atMinimum = new ExternalPaymentProperties(
                Duration.ofMillis(1),
                threadPool,
                new ExternalCircuitBreakerProperties(
                        50, 20, 10, Duration.ofSeconds(1), 1));

        assertThat(validator.validate(belowMinimum))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("circuitBreaker.waitDurationOpen");
        assertThat(validator.validate(atMinimum)).isEmpty();
    }

    @DisplayName("외부 연동 CircuitBreaker는 최소 호출 수가 슬라이딩 윈도우를 넘지 못한다")
    @Test
    void circuitBreaker_minimumCallsAboveWindow_rejected() {
        var invalid = new ExternalPaymentProperties(
                Duration.ofMillis(1),
                new ExternalPaymentProperties.ThreadPool(1, 1),
                new ExternalCircuitBreakerProperties(
                        50, 9, 10, Duration.ofSeconds(30), 3));

        assertThat(validator.validate(invalid))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("circuitBreaker.minimumNumberOfCallsWithinWindow");
    }

    @DisplayName("게스트 토큰 Duration은 기존 시간 최소 단위를 유지한다")
    @Test
    void guestTokenExpiry_preservesHourMinimum() {
        var belowMinimum = new GuestTokenProperties(
                HMAC_SECRET, "", Duration.ofMinutes(59), Duration.ofHours(1));
        var atMinimum = new GuestTokenProperties(
                HMAC_SECRET, "", Duration.ofHours(1), Duration.ofHours(1));

        assertThat(validator.validate(belowMinimum))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("accessExpiry");
        assertThat(validator.validate(atMinimum)).isEmpty();
    }

    @DisplayName("외부 호출 Duration 0밀리초 설정은 실제 프로퍼티 바인딩에서 거절한다")
    @Test
    void externalTimeout_zeroProperty_failsBinding() {
        contextRunner
                .withPropertyValues("app.external.email-verification.timeout=0ms")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining(
                                    "app.external.email-verification.timeout");
                });
    }

    @DisplayName("중첩 CircuitBreaker Duration 0초 설정은 실제 프로퍼티 바인딩에서 거절한다")
    @Test
    void circuitBreakerWaitDuration_zeroProperty_failsBinding() {
        contextRunner
                .withPropertyValues(
                        "app.external.payment.circuit-breaker.wait-duration-open=0s")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining(
                                    "app.external.payment.circuitBreaker.waitDurationOpen");
                });
    }

    @DisplayName("게스트 토큰 Duration 0시간 설정은 실제 프로퍼티 바인딩에서 거절한다")
    @Test
    void guestTokenExpiry_zeroProperty_failsBinding() {
        contextRunner
                .withPropertyValues("app.guest-token.access-expiry=0h")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("app.guest-token.access-expiry");
                });
    }

    @DisplayName("이전 게스트 토큰 키가 짧으면 실제 프로퍼티 바인딩에서 거절한다")
    @Test
    void guestTokenPreviousSecret_shortProperty_failsBinding() {
        contextRunner
                .withPropertyValues("app.guest-token.previous-hmac-secret=short-secret")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("app.guest-token.previous-hmac-secret");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            EmailVerificationProperties.class,
            ExternalPaymentProperties.class,
            GuestTokenProperties.class
    })
    static class DurationPropertiesConfig {}
}
