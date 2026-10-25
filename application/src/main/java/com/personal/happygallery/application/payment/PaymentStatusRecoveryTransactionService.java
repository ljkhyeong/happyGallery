package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.PhoneVerificationConsumptionService;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase.RecoveredPayment;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase.RecoveryResult;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.token.GuestTokenProperties;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.application.token.GuestTokenService.IssuedToken;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentStatusRecoveryTransactionService {

    private static final List<PaymentAttemptStatus> TERMINAL_STATUSES = Arrays.stream(PaymentAttemptStatus.values())
            .filter(PaymentAttemptStatus::isSensitiveDataCleanupAllowed)
            .toList();

    private final PhoneVerificationConsumptionService phoneVerification;
    private final PaymentAttemptReaderPort attemptReader;
    private final RefundPort refundPort;
    private final BlindIndexKeyRing blindIndexKeyRing;
    private final GuestTokenService guestTokenService;
    private final GuestTokenProperties guestTokenProperties;
    private final CustomerPaymentStatusResolver statusResolver;
    private final Clock clock;

    PaymentStatusRecoveryTransactionService(
            PhoneVerificationConsumptionService phoneVerification,
            PaymentAttemptReaderPort attemptReader,
            RefundPort refundPort,
            BlindIndexKeyRing blindIndexKeyRing,
            GuestTokenService guestTokenService,
            GuestTokenProperties guestTokenProperties,
            CustomerPaymentStatusResolver statusResolver,
            Clock clock) {
        this.phoneVerification = phoneVerification;
        this.attemptReader = attemptReader;
        this.refundPort = refundPort;
        this.blindIndexKeyRing = blindIndexKeyRing;
        this.guestTokenService = guestTokenService;
        this.guestTokenProperties = guestTokenProperties;
        this.statusResolver = statusResolver;
        this.clock = clock;
    }

    /** 이미 시도 제한을 통과한 인증 코드를 독립 트랜잭션에서 소비한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verifyPhoneOwnership(String normalizedPhone, String verificationCode) {
        phoneVerification.consume(
                normalizedPhone,
                verificationCode,
                PhoneVerificationPurpose.GUEST_PAYMENT_STATUS_RECOVERY);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecoveryResult recover(String phone) {
        List<String> phoneHmacCandidates = blindIndexKeyRing.indexCandidates(phone);
        LocalDateTime terminalCutoff = LocalDateTime.ofInstant(
                clock.instant().minus(guestTokenProperties.accessExpiry()), ZoneOffset.UTC);
        List<PaymentAttempt> attempts = attemptReader.findGuestRecoveryCandidatesForUpdate(
                phoneHmacCandidates, TERMINAL_STATUSES, terminalCutoff);
        if (attempts.isEmpty()) {
            throw new NotFoundException("결제");
        }

        List<Long> compensationAttemptIds = attempts.stream()
                .filter(statusResolver::isCompensationStatus)
                .map(PaymentAttempt::getId)
                .toList();
        Map<Long, Refund> refundsByPaymentAttemptId = new HashMap<>();
        if (!compensationAttemptIds.isEmpty()) {
            for (Refund refund : refundPort.findByPaymentAttemptIdIn(compensationAttemptIds)) {
                refundsByPaymentAttemptId.put(refund.getPaymentAttemptId(), refund);
            }
        }

        IssuedToken token = guestTokenService.issuePaymentStatusToken();
        List<RecoveredPayment> recoveredPayments = new ArrayList<>(attempts.size());
        for (PaymentAttempt attempt : attempts) {
            attempt.replaceStatusAccessToken(token.tokenHash());
            recoveredPayments.add(new RecoveredPayment(
                    attempt.getOrderIdExternal(),
                    attempt.getContext(),
                    attempt.getAmount(),
                    statusResolver.resolve(
                            attempt,
                            Optional.ofNullable(refundsByPaymentAttemptId.get(attempt.getId())))));
        }
        return new RecoveryResult(
                token.rawToken(),
                token.expiresAt(),
                recoveredPayments);
    }
}
