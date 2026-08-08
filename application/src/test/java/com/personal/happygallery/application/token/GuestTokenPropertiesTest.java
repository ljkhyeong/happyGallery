package com.personal.happygallery.application.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestTokenPropertiesTest {

    private static final String ACTIVE_SECRET = "active-guest-token-secret-at-least-32-bytes";

    @DisplayName("이전 키는 비어 있거나 32자 이상이어야 한다")
    @Test
    void constructor_previousSecret_acceptsBlankOrAtLeast32Characters() {
        assertThatNoException()
                .isThrownBy(() -> new GuestTokenProperties(
                        ACTIVE_SECRET, "", Duration.ofHours(720), Duration.ofHours(24)));
        assertThatThrownBy(() -> new GuestTokenProperties(
                ACTIVE_SECRET, "short-secret", Duration.ofHours(720), Duration.ofHours(24)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이전 게스트 토큰 HMAC 키는 32자 이상이어야 합니다.");
    }

    @DisplayName("활성 키와 이전 키는 서로 달라야 한다")
    @Test
    void constructor_sameSecrets_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new GuestTokenProperties(
                ACTIVE_SECRET, ACTIVE_SECRET, Duration.ofHours(720), Duration.ofHours(24)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("활성 키와 이전 게스트 토큰 HMAC 키는 달라야 합니다.");
    }

    @DisplayName("일반 비회원과 결제 상태 토큰의 공통 보존 기간을 Duration으로 제공한다")
    @Test
    void accessExpiry_returnsConfiguredDuration() {
        GuestTokenProperties properties = new GuestTokenProperties(
                ACTIVE_SECRET, "", Duration.ofHours(720), Duration.ofHours(24));

        assertThat(properties.accessExpiry()).isEqualTo(Duration.ofDays(30));
    }
}
