package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.EmailVerificationStorePort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.user.EmailVerification;
import com.personal.happygallery.domain.user.User;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class EmailVerificationIssueTransactionService {

    private final UserReaderPort userReader;
    private final EmailVerificationStorePort verificationStore;

    EmailVerificationIssueTransactionService(
            UserReaderPort userReader,
            EmailVerificationStorePort verificationStore
    ) {
        this.userReader = userReader;
        this.verificationStore = verificationStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailVerification create(
            Long userId,
            long credentialVersion,
            String email,
            String code,
            LocalDateTime expiresAt,
            boolean recentlyReauthenticated
    ) {
        User user = userReader.findByIdForUpdate(userId)
                .orElseThrow(NotFoundException.supplier("회원"));
        requireCurrentAuthentication(user, credentialVersion, recentlyReauthenticated);
        requireEmailAvailable(user, email);
        return verificationStore.save(new EmailVerification(
                userId, credentialVersion, email, code, expiresAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailVerification completeDelivery(EmailVerification issued) {
        EmailVerification verification = verificationStore.findByIdForUpdate(
                        issued.getId(),
                        issued.getUserId(),
                        issued.getCredentialVersion(),
                        issued.getEmail())
                .orElseThrow(() -> new IllegalStateException(
                        "발급한 이메일 인증 코드를 찾을 수 없습니다."));
        if (verification.isVerified()) {
            return verification;
        }

        verificationStore.invalidateEarlierUnconsumed(
                verification.getUserId(),
                verification.getId());
        verification.markDelivered();
        return verificationStore.save(verification);
    }

    private void requireEmailAvailable(User user, String email) {
        if (user.getEmail() != null || userReader.existsByEmail(email)) {
            throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private static void requireCurrentAuthentication(
            User user,
            long credentialVersion,
            boolean recentlyReauthenticated
    ) {
        if (user.getCredentialVersion() != credentialVersion) {
            throw new HappyGalleryException(ErrorCode.UNAUTHORIZED);
        }
        if (!recentlyReauthenticated) {
            throw new HappyGalleryException(ErrorCode.REAUTHENTICATION_REQUIRED);
        }
    }
}
