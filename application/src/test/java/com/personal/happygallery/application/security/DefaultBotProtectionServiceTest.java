package com.personal.happygallery.application.security;

import com.personal.happygallery.application.security.port.out.BotChallengeVerifier;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class DefaultBotProtectionServiceTest {
    private final BotChallengeVerifier verifier = mock(BotChallengeVerifier.class);
    private final DefaultBotProtectionService service = new DefaultBotProtectionService(verifier);

    @Test
    @DisplayName("비활성 상태에서는 외부 검증 없이 기존 요청을 처리한다")
    void disabled() {
        assertThatCode(() -> service.verify(null, "group_inquiry")).doesNotThrowAnyException();
        verify(verifier, never()).verify(any(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("활성 상태에서 토큰이 없으면 외부 호출 전에 거절한다")
    void missingToken(String token) {
        when(verifier.siteKey()).thenReturn("site-key");
        assertThatThrownBy(() -> service.verify(token, "phone_verification"))
                .isInstanceOf(HappyGalleryException.class);
        verify(verifier, never()).verify(any(), any());
    }

    @Test
    @DisplayName("너무 긴 토큰은 외부 호출 전에 거절한다")
    void oversizedToken() {
        when(verifier.siteKey()).thenReturn("site-key");
        assertThatThrownBy(() -> service.verify("x".repeat(2049), "group_inquiry"))
                .isInstanceOf(HappyGalleryException.class);
        verify(verifier, never()).verify(any(), any());
    }

    @Test
    @DisplayName("서버가 정한 용도의 검증을 통과한 요청만 허용한다")
    void verifiedAction() {
        when(verifier.siteKey()).thenReturn("site-key");
        when(verifier.verify("token", "phone_verification")).thenReturn(true);
        assertThatCode(() -> service.verify("token", "phone_verification")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.verify("token", "group_inquiry"))
                .isInstanceOf(HappyGalleryException.class);
    }
}
