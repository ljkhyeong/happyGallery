package com.personal.happygallery.application.customer;

import com.personal.happygallery.adapter.out.persistence.user.SocialAccountRepository;
import com.personal.happygallery.adapter.out.persistence.user.UserRepository;
import com.personal.happygallery.adapter.out.persistence.policy.PolicyConsentRepository;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLinkCommand;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialUnlinkCommand;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialReauthenticationCommand;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.policy.PolicyConsentPurpose;
import com.personal.happygallery.domain.policy.PolicyConsentType;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;

@UseCaseIT
class SocialAuthUseCaseIT {

    @Autowired SocialAuthUseCase socialAuth;
    @Autowired SocialAccountStorePort socialAccountStore;
    @Autowired SocialAccountRepository socialAccountRepository;
    @Autowired PolicyConsentRepository policyConsentRepository;
    @Autowired UserRepository userRepository;
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
                "테스트 네이버 사용자",
                acceptedPolicies()));
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
            softly.assertThat(policyConsentRepository.findByUserIdOrderById(firstLogin.user().getId()))
                    .satisfiesExactly(
                            consent -> assertSoftly(consentSoftly -> {
                                consentSoftly.assertThat(consent.getType())
                                        .isEqualTo(PolicyConsentType.TERMS_OF_SERVICE);
                                consentSoftly.assertThat(consent.getPurpose())
                                        .isEqualTo(PolicyConsentPurpose.SOCIAL_SIGNUP);
                                consentSoftly.assertThat(consent.getPolicyVersion())
                                        .isEqualTo("2026-08-08-v1");
                            }),
                            consent -> assertSoftly(consentSoftly -> {
                                consentSoftly.assertThat(consent.getType())
                                        .isEqualTo(PolicyConsentType.PRIVACY_POLICY);
                                consentSoftly.assertThat(consent.getPurpose())
                                        .isEqualTo(PolicyConsentPurpose.SOCIAL_SIGNUP);
                                consentSoftly.assertThat(consent.getPolicyVersion())
                                        .isEqualTo("2026-08-11-v2");
                            }));
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
                "테스트 구글 사용자",
                acceptedPolicies()));

        assertThatThrownBy(() -> socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "another-google-account-id",
                "social-test@example.com",
                "다른 구글 사용자")))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
    }

    @DisplayName("검증된 카카오 이메일이 기존 회원과 같으면 자동 연결하지 않는다")
    @Test
    void rejectsKakaoAccountAutoLinkByEmail() {
        socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.KAKAO,
                "kakao-account-id",
                "kakao-social@example.com",
                "카카오 사용자",
                acceptedPolicies()));

        assertThatThrownBy(() -> socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.KAKAO,
                "another-kakao-account-id",
                "kakao-social@example.com",
                "다른 카카오 사용자")))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
    }

    @DisplayName("유효한 정책 동의가 없는 최초 소셜 로그인은 회원을 만들지 않는다")
    @Test
    void rejectsFirstSocialLoginWithoutPolicyConsent() {
        assertThatThrownBy(() -> socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "google-without-consent",
                "without-consent@example.com",
                "동의 없는 사용자")))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.POLICY_CONSENT_REQUIRED);

        assertSoftly(softly -> {
            softly.assertThat(userRepository.count()).isZero();
            softly.assertThat(socialAccountRepository.count()).isZero();
            softly.assertThat(policyConsentRepository.count()).isZero();
        });
    }

    @DisplayName("같은 Google 이메일의 동시 최초 로그인은 한 회원만 만들고 다른 요청에 연결 필요를 알린다")
    @Test
    void concurrentFirstLogin_sameGoogleEmail_createsOneUser() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<LoginOutcome> outcomes;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> loginAfter(
                    ready, start, "concurrent-google-a", "same-google@example.com"));
            var second = executor.submit(() -> loginAfter(
                    ready, start, "concurrent-google-b", "same-google@example.com"));
            ready.await();
            start.countDown();
            outcomes = List.of(first.get(), second.get());
        }

        assertSoftly(softly -> {
            softly.assertThat(outcomes)
                    .extracting(LoginOutcome::created)
                    .containsExactlyInAnyOrder(true, false);
            softly.assertThat(outcomes)
                    .extracting(LoginOutcome::errorCode)
                    .containsExactlyInAnyOrder(null, ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
            softly.assertThat(userRepository.count()).isOne();
            softly.assertThat(socialAccountRepository.count()).isOne();
        });
    }

    @DisplayName("같은 제공자 계정의 DB 유일 제약은 명시적인 소셜 계정 충돌로 번역한다")
    @Test
    void saveSocialAccount_duplicateProviderIdentity_returnsConflict() {
        var winner = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "shared-naver-account",
                null,
                "첫 번째 사용자",
                acceptedPolicies()));
        var loser = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "other-google-account",
                "other-google@example.com",
                "두 번째 사용자",
                acceptedPolicies()));

        assertThatThrownBy(() -> socialAccountStore.save(new SocialAccount(
                loser.user().getId(), SocialProvider.NAVER, "shared-naver-account")))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);

        assertSoftly(softly -> {
            softly.assertThat(socialAuth.listLinkedProviders(winner.user().getId()))
                    .containsExactly(SocialProvider.NAVER);
            softly.assertThat(socialAuth.listLinkedProviders(loser.user().getId()))
                    .containsExactly(SocialProvider.GOOGLE);
            softly.assertThat(socialAccountRepository.count()).isEqualTo(2);
        });
    }

    @DisplayName("로그인한 회원은 소셜 계정을 명시적으로 연결하고 마지막 로그인 수단은 해제하지 못한다")
    @Test
    void linksAndSafelyUnlinksSocialAccounts() {
        var naverLogin = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "naver-account-id",
                "social-link@example.com",
                "소셜 연결 사용자",
                acceptedPolicies()));

        socialAuth.linkSocialAccount(new SocialLinkCommand(
                naverLogin.user().getId(),
                naverLogin.user().getCredentialVersion(),
                SocialProvider.GOOGLE,
                "google-account-id",
                true));
        long linkedCredentialVersion = userRepository.findById(naverLogin.user().getId())
                .orElseThrow()
                .getCredentialVersion();
        socialAuth.unlinkSocialAccount(new SocialUnlinkCommand(
                naverLogin.user().getId(),
                linkedCredentialVersion,
                SocialProvider.NAVER,
                true));

        assertThat(socialAuth.listLinkedProviders(naverLogin.user().getId()))
                .containsExactly(SocialProvider.GOOGLE);
        long unlinkedCredentialVersion = userRepository.findById(naverLogin.user().getId())
                .orElseThrow()
                .getCredentialVersion();
        assertThatThrownBy(() -> socialAuth.unlinkSocialAccount(new SocialUnlinkCommand(
                naverLogin.user().getId(),
                unlinkedCredentialVersion,
                SocialProvider.GOOGLE,
                true)))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.LAST_LOGIN_METHOD_REQUIRED);
    }

    @DisplayName("소셜 재인증은 현재 회원에게 연결된 동일 제공자 식별자만 허용한다")
    @Test
    void verifiesExactLinkedSocialIdentity() {
        var login = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "reauthentication-google-id",
                "social-reauthentication@example.com",
                "소셜 재인증 사용자",
                acceptedPolicies()));
        SocialReauthenticationCommand matching = new SocialReauthenticationCommand(
                login.user().getId(),
                login.user().getCredentialVersion(),
                SocialProvider.GOOGLE,
                "reauthentication-google-id");

        assertThatCode(() -> socialAuth.verifyLinkedSocialAccount(matching))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> socialAuth.verifyLinkedSocialAccount(
                new SocialReauthenticationCommand(
                        login.user().getId(),
                        login.user().getCredentialVersion(),
                        SocialProvider.GOOGLE,
                        "different-google-id")))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    private LoginOutcome loginAfter(CountDownLatch ready,
                                    CountDownLatch start,
                                    String providerId,
                                    String email) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            var result = socialAuth.socialLogin(new SocialLoginCommand(
                    SocialProvider.GOOGLE, providerId, email, "동시 로그인", acceptedPolicies()));
            return new LoginOutcome(result.newUser(), null);
        } catch (HappyGalleryException exception) {
            return new LoginOutcome(false, exception.getErrorCode());
        }
    }

    private record LoginOutcome(boolean created, ErrorCode errorCode) {}

}
