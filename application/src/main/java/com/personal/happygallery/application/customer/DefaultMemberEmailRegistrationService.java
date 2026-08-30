package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.MemberEmailRegistrationUseCase;
import com.personal.happygallery.application.customer.port.out.EmailVerificationRateLimitGuard;
import com.personal.happygallery.application.customer.port.out.EmailVerificationSender;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.EmailAddress;
import com.personal.happygallery.domain.user.EmailVerification;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultMemberEmailRegistrationService implements MemberEmailRegistrationUseCase {

    private static final int VERIFICATION_EXPIRE_MINUTES = 5;

    private final EmailVerificationIssueTransactionService issueTransaction;
    private final MemberEmailRegistrationTransactionService registrationTransaction;
    private final EmailVerificationRateLimitGuard rateLimitGuard;
    private final EmailVerificationSender verificationSender;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public DefaultMemberEmailRegistrationService(
            EmailVerificationIssueTransactionService issueTransaction,
            MemberEmailRegistrationTransactionService registrationTransaction,
            EmailVerificationRateLimitGuard rateLimitGuard,
            EmailVerificationSender verificationSender,
            Clock clock
    ) {
        this.issueTransaction = issueTransaction;
        this.registrationTransaction = registrationTransaction;
        this.rateLimitGuard = rateLimitGuard;
        this.verificationSender = verificationSender;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendVerificationCode(SendVerificationCommand command) {
        String email = EmailAddress.required(command.email());
        rateLimitGuard.checkIssue(command.userId(), email);
        String code = "%06d".formatted(random.nextInt(1_000_000));
        EmailVerification issued = issueTransaction.create(
                command.userId(),
                command.credentialVersion(),
                email,
                code,
                LocalDateTime.now(clock).plusMinutes(VERIFICATION_EXPIRE_MINUTES),
                command.recentlyReauthenticated());
        if (!verificationSender.send(email, code)) {
            throw new HappyGalleryException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    "인증 코드를 발송하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
        issueTransaction.completeDelivery(issued);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void registerVerifiedEmail(RegisterEmailCommand command) {
        String email = EmailAddress.required(command.email());
        rateLimitGuard.checkAttempt(command.userId(), email);
        registrationTransaction.register(command, email);
    }
}
