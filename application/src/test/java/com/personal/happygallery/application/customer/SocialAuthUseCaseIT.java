package com.personal.happygallery.application.customer;

import com.personal.happygallery.adapter.out.persistence.user.SocialAccountRepository;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLinkCommand;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
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

    @DisplayName("네이버 신규 계정은 프로필 이메일을 저장하지 않고 제공자 ID로 다시 로그인한다")
    @Test
    void doesNotPersistNaverProfileEmail() {
        var firstLogin = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "naver-account-id",
                "social-test@example.com",
                "테스트 네이버 사용자"));
        var secondLogin = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "naver-account-id",
                "changed-profile@example.com",
                "테스트 네이버 사용자"));
        var storedSocialAccount = socialAccountRepository.findAll().getFirst();

        assertSoftly(softly -> {
            softly.assertThat(firstLogin.newUser()).isTrue();
            softly.assertThat(secondLogin.newUser()).isFalse();
            softly.assertThat(secondLogin.user().getId()).isEqualTo(firstLogin.user().getId());
            softly.assertThat(firstLogin.user().getEmail()).isNull();
            softly.assertThat(firstLogin.user().getEmailEnc()).isNull();
            softly.assertThat(firstLogin.user().getEmailHmac()).isNull();
            softly.assertThat(socialAccountRepository.count()).isEqualTo(1);
            softly.assertThat(storedSocialAccount.getProviderIdEnc())
                    .isNotBlank()
                    .doesNotContain("naver-account-id");
        });
    }

    @DisplayName("검증된 Google 이메일이 기존 회원과 같으면 자동 연결하지 않는다")
    @Test
    void rejectsSocialAccountAutoLinkByEmail() {
        socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "google-account-id",
                "social-test@example.com",
                "테스트 구글 사용자"));

        assertThatThrownBy(() -> socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "another-google-account-id",
                "social-test@example.com",
                "다른 구글 사용자")))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
    }

    @DisplayName("로그인한 회원은 소셜 계정을 명시적으로 연결하고 마지막 로그인 수단은 해제하지 못한다")
    @Test
    void linksAndSafelyUnlinksSocialAccounts() {
        var naverLogin = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "naver-account-id",
                "social-link@example.com",
                "소셜 연결 사용자"));

        socialAuth.linkSocialAccount(new SocialLinkCommand(
                naverLogin.user().getId(),
                naverLogin.user().getCredentialVersion(),
                SocialProvider.GOOGLE,
                "google-account-id"));
        socialAuth.unlinkSocialAccount(naverLogin.user().getId(), SocialProvider.NAVER);

        assertThat(socialAuth.listLinkedProviders(naverLogin.user().getId()))
                .containsExactly(SocialProvider.GOOGLE);
        assertThatThrownBy(() -> socialAuth.unlinkSocialAccount(
                naverLogin.user().getId(), SocialProvider.GOOGLE))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.LAST_LOGIN_METHOD_REQUIRED);
    }

}
