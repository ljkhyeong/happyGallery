package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.MemberEmailRegistrationUseCase.RegisterEmailCommand;
import com.personal.happygallery.application.customer.port.out.EmailVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.EmailVerificationStorePort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.user.EmailVerification;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MemberEmailRegistrationTransactionService {

    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final EmailVerificationReaderPort verificationReader;
    private final EmailVerificationStorePort verificationStore;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    MemberEmailRegistrationTransactionService(
            UserReaderPort userReader,
            UserStorePort userStore,
            EmailVerificationReaderPort verificationReader,
            EmailVerificationStorePort verificationStore,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.verificationReader = verificationReader;
        this.verificationStore = verificationStore;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void register(RegisterEmailCommand command, String normalizedEmail) {
        User user = userReader.findByIdForUpdate(command.userId())
                .orElseThrow(NotFoundException.supplier("회원"));
        requireCurrentAuthentication(user, command);
        if (user.getEmail() != null) {
            throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        EmailVerification verification = verificationReader.findValidVerification(
                        command.userId(),
                        command.credentialVersion(),
                        normalizedEmail,
                        command.verificationCode(),
                        LocalDateTime.now(clock))
                .orElseThrow(() -> new HappyGalleryException(
                        ErrorCode.EMAIL_VERIFICATION_FAILED));
        verification.markVerified();
        verificationStore.save(verification);

        long invalidatedCredentialVersion = user.getCredentialVersion();
        user.registerVerifiedEmail(normalizedEmail);
        user.markAuthenticationMethodsChanged();
        userStore.saveAndFlush(user);
        eventPublisher.publishEvent(new CustomerCredentialsChangedEvent(
                user.getId(), invalidatedCredentialVersion));
    }

    private static void requireCurrentAuthentication(User user, RegisterEmailCommand command) {
        if (user.getCredentialVersion() != command.credentialVersion()) {
            throw new HappyGalleryException(ErrorCode.UNAUTHORIZED);
        }
        if (!command.recentlyReauthenticated()) {
            throw new HappyGalleryException(ErrorCode.REAUTHENTICATION_REQUIRED);
        }
    }
}
