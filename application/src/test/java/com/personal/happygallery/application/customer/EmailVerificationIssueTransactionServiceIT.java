package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.application.customer.port.out.EmailVerificationReaderPort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.user.EmailVerification;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class EmailVerificationIssueTransactionServiceIT {

    @Autowired EmailVerificationIssueTransactionService issueTransaction;
    @Autowired EmailVerificationReaderPort verificationReader;
    @Autowired SocialAuthUseCase socialAuth;
    @Autowired BlindIndexer blindIndexer;
    @Autowired Clock clock;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearUsers();
    }

    @Test
    @DisplayName("늦게 끝난 과거 이메일 발송은 최신 인증 코드를 다시 활성화하지 않는다")
    void olderDeliveryCompletionCannotOverrideLatestCode() {
        User user = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "email-delivery-order-naver-id",
                null,
                "네이버 회원",
                acceptedPolicies())).user();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(5);
        String oldEmail = "old-email@example.com";
        String newEmail = "new-email@example.com";
        String oldCode = "111111";
        String newCode = "222222";

        EmailVerification older = issueTransaction.create(
                user.getId(), user.getCredentialVersion(),
                oldEmail, oldCode, expiresAt, true);
        EmailVerification newer = issueTransaction.create(
                user.getId(), user.getCredentialVersion(),
                newEmail, newCode, expiresAt, true);

        EmailVerification deliveredNewer = issueTransaction.completeDelivery(newer);
        EmailVerification completedOlder = issueTransaction.completeDelivery(older);
        Optional<EmailVerification> oldVerification = findValidVerification(
                user, oldEmail, oldCode);
        Optional<EmailVerification> newVerification = findValidVerification(
                user, newEmail, newCode);

        assertSoftly(softly -> {
            softly.assertThat(deliveredNewer.isDelivered()).isTrue();
            softly.assertThat(deliveredNewer.getCodeHmac()).isEqualTo(
                    blindIndexer.index(
                            user.getId() + ":" + newEmail + ":" + newCode));
            softly.assertThat(deliveredNewer.getCodeEnc()).isNotEqualTo(newCode);
            softly.assertThat(completedOlder.isVerified()).isTrue();
            softly.assertThat(completedOlder.isDelivered()).isFalse();
            softly.assertThat(oldVerification).isEmpty();
            softly.assertThat(newVerification).isPresent();
        });
    }

    private Optional<EmailVerification> findValidVerification(
            User user,
            String email,
            String code
    ) {
        return new TransactionTemplate(transactionManager).execute(status ->
                verificationReader.findValidVerification(
                        user.getId(),
                        user.getCredentialVersion(),
                        email,
                        code,
                        LocalDateTime.now(clock)));
    }
}
