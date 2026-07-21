package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase.RecoveredPayment;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase.RecoveryResult;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.token.GuestTokenProperties;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.application.token.GuestTokenService.IssuedToken;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentStatusRecoveryTransactionService {

    private static final List<PaymentAttemptStatus> TERMINAL_STATUSES = Arrays.stream(PaymentAttemptStatus.values())
            .filter(PaymentAttemptStatus::isSensitiveDataCleanupAllowed)
            .toList();

    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    private final PaymentAttemptReaderPort attemptReader;
    private final BlindIndexKeyRing blindIndexKeyRing;
    private final GuestTokenService guestTokenService;
    private final GuestTokenProperties guestTokenProperties;
    private final CustomerPaymentStatusResolver statusResolver;
    private final Clock clock;

    PaymentStatusRecoveryTransactionService(
            PhoneOwnershipVerificationUseCase phoneOwnershipVerification,
            PaymentAttemptReaderPort attemptReader,
            BlindIndexKeyRing blindIndexKeyRing,
            GuestTokenService guestTokenService,
            GuestTokenProperties guestTokenProperties,
            CustomerPaymentStatusResolver statusResolver,
            Clock clock) {
        this.phoneOwnershipVerification = phoneOwnershipVerification;
        this.attemptReader = attemptReader;
        this.blindIndexKeyRing = blindIndexKeyRing;
        this.guestTokenService = guestTokenService;
        this.guestTokenProperties = guestTokenProperties;
        this.statusResolver = statusResolver;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verifyPhoneOwnership(String phone, String verificationCode) {
        phoneOwnershipVerification.verify(phone, verificationCode);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecoveryResult recover(String phone) {
        List<String> phoneHmacCandidates = blindIndexKeyRing.indexCandidates(phone);
        LocalDateTime terminalCutoff = LocalDateTime.ofInstant(
                clock.instant().minus(guestTokenProperties.accessExpiry()), ZoneOffset.UTC);
        List<PaymentAttempt> attempts = attemptReader.findGuestRecoveryCandidatesForUpdate(
                        phoneHmacCandidates, TERMINAL_STATUSES, terminalCutoff)
                .stream()
                .filter(attempt -> matchesAnyHmac(attempt.getOwnerPhoneHmac(), phoneHmacCandidates))
                .toList();
        if (attempts.isEmpty()) {
            throw new NotFoundException("결제");
        }

        IssuedToken token = guestTokenService.issuePaymentStatusToken();
        attempts.forEach(attempt -> attempt.replaceStatusAccessToken(token.tokenHash()));
        return new RecoveryResult(
                token.rawToken(),
                token.expiresAt(),
                attempts.stream()
                        .map(attempt -> new RecoveredPayment(
                                attempt.getOrderIdExternal(),
                                attempt.getContext(),
                                attempt.getAmount(),
                                statusResolver.resolve(attempt)))
                        .toList());
    }

    private boolean matchesAnyHmac(String storedHmac, List<String> candidates) {
        byte[] stored = storedHmac == null
                ? new byte[0]
                : storedHmac.getBytes(StandardCharsets.UTF_8);
        boolean matched = false;
        for (String candidate : candidates) {
            matched |= MessageDigest.isEqual(stored, candidate.getBytes(StandardCharsets.UTF_8));
        }
        return matched;
    }
}
