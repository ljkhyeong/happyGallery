package com.personal.happygallery.application.customer;

import com.personal.happygallery.adapter.out.persistence.user.SocialAccountRepository;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class SocialAuthUseCaseIT {

    @Autowired SocialAuthUseCase socialAuth;
    @Autowired SocialAccountRepository socialAccountRepository;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearUsers();
    }

    @DisplayName("네이버 계정은 처음 로그인할 때 가입되고 이후 같은 계정으로 로그인된다")
    @Test
    void logsInWithExistingNaverAccount() {
        var firstLogin = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "naver-code",
                "https://happygallery.example/auth/callback/naver",
                "naver-state"));
        var secondLogin = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "naver-code",
                "https://happygallery.example/auth/callback/naver",
                "naver-state"));

        assertSoftly(softly -> {
            softly.assertThat(firstLogin.newUser()).isTrue();
            softly.assertThat(secondLogin.newUser()).isFalse();
            softly.assertThat(secondLogin.user().getId()).isEqualTo(firstLogin.user().getId());
            softly.assertThat(socialAccountRepository.count()).isEqualTo(1);
        });
    }

    @DisplayName("소셜 이메일이 기존 회원과 같으면 제공자와 관계없이 자동 연결하지 않는다")
    @Test
    void rejectsSocialAccountAutoLinkByEmail() {
        socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "naver-code",
                "https://happygallery.example/auth/callback/naver",
                "naver-state"));

        assertThatThrownBy(() -> socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "google-code",
                "https://happygallery.example/auth/callback/google",
                "google-state")))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
    }

}
