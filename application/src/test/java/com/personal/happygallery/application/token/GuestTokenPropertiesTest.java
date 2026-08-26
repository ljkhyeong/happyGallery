package com.personal.happygallery.application.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestTokenPropertiesTest {

    private static final String ACTIVE_SECRET = "active-guest-token-secret-at-least-32-bytes";

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @DisplayName("이전 키는 32자 미만이면 거절한다")
    @Test
    void validation_shortPreviousSecret_hasViolation() {
        GuestTokenProperties properties = new GuestTokenProperties(
                ACTIVE_SECRET, "short-secret", Duration.ofHours(720), Duration.ofHours(24));

        assertThat(validator.validate(properties))
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("previousHmacSecret");
                    assertThat(violation.getMessage())
                            .isEqualTo("이전 게스트 토큰 HMAC 키는 32자 이상이어야 합니다.");
                });
    }

    @DisplayName("활성 키와 이전 키는 서로 달라야 한다")
    @Test
    void constructor_sameSecrets_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new GuestTokenProperties(
                ACTIVE_SECRET, ACTIVE_SECRET, Duration.ofHours(720), Duration.ofHours(24)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("활성 키와 이전 게스트 토큰 HMAC 키는 달라야 합니다.");
    }

}
