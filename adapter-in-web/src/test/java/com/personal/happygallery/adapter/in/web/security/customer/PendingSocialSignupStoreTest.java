package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PendingSocialSignupStoreTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);
    private final SessionStateCodec codec = new SessionStateCodec(JsonMapper.builder().build());
    private final PendingSocialSignupStore store = new PendingSocialSignupStore(clock, codec);
    private final SocialLoginCommand profile = new SocialLoginCommand(SocialProvider.NAVER, "naver-id", null, "회원");

    @Test
    @DisplayName("인증 결과는 같은 세션과 시도에서 한 번만 사용할 수 있다")
    void bindsSessionAndAttemptAndConsumesOnce() {
        var request = new MockHttpServletRequest();
        String attempt = store.save(request, profile);
        assertThat(request.getSession().getAttribute("pendingSocialSignup")).isInstanceOf(String.class);
        assertThatThrownBy(() -> store.consume(new MockHttpServletRequest(), attempt))
                .isInstanceOf(HappyGalleryException.class);
        assertThatThrownBy(() -> store.consume(request, "other-attempt"))
                .isInstanceOf(HappyGalleryException.class);
        assertThat(store.consume(request, attempt)).isEqualTo(profile);
        assertThatThrownBy(() -> store.consume(request, attempt)).isInstanceOf(HappyGalleryException.class);
    }

    @Test
    @DisplayName("가입 대기는 만료 직전까지 유효하고 5분 정각에는 거절한다")
    void expiresAtFiveMinutes() {
        var before = new MockHttpServletRequest();
        String beforeId = store.save(before, profile);
        var beforeStore = new PendingSocialSignupStore(Clock.offset(clock, Duration.ofSeconds(299)), codec);
        assertThat(beforeStore.consume(before, beforeId)).isEqualTo(profile);
        var expired = new MockHttpServletRequest();
        String expiredId = store.save(expired, profile);
        var expiredStore = new PendingSocialSignupStore(Clock.offset(clock, Duration.ofMinutes(5)), codec);
        assertThatThrownBy(() -> expiredStore.consume(expired, expiredId)).isInstanceOf(HappyGalleryException.class);
        assertThat(expired.getSession().getAttribute("pendingSocialSignup")).isNull();
    }

    @Test
    @DisplayName("새 계정으로 인증을 시작하면 이전 가입 시도로 새 계정을 가입시킬 수 없다")
    void replacesPreviousAttempt() {
        var request = new MockHttpServletRequest();
        String oldAttempt = store.save(request, profile);
        var next = new SocialLoginCommand(SocialProvider.GOOGLE, "google-id", "member@example.com", "새 회원");
        String newAttempt = store.save(request, next);
        assertThatThrownBy(() -> store.consume(request, oldAttempt)).isInstanceOf(HappyGalleryException.class);
        assertThat(store.consume(request, newAttempt)).isEqualTo(next);
    }
}
