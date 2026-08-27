package com.personal.happygallery.adapter.out.external.oauth;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SocialOAuth2ClientConfigTest {

    private final SocialOAuth2ClientConfig config = new SocialOAuth2ClientConfig(mock());

    @DisplayName("카카오 회원 정보의 계정과 프로필 속성을 로그인 프로필로 펼친다")
    @Test
    void flattensKakaoAccountAndProfile() {
        Map<String, Object> flattened = config.flattenKakaoResponse(Map.of(
                "id", 123456789L,
                "kakao_account", Map.of(
                        "email", "kakao@example.com",
                        "is_email_valid", true,
                        "is_email_verified", true,
                        "profile", Map.of("nickname", "카카오 사용자"))));

        assertThat(flattened).containsEntry("id", 123456789L)
                .containsEntry("email", "kakao@example.com")
                .containsEntry("is_email_valid", true)
                .containsEntry("is_email_verified", true)
                .containsEntry("nickname", "카카오 사용자");
    }

    @DisplayName("카카오 계정 속성이 없는 회원 정보는 인증 실패로 처리한다")
    @Test
    void rejectsKakaoResponseWithoutAccount() {
        assertThatThrownBy(() -> config.flattenKakaoResponse(Map.of("id", 123456789L)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
