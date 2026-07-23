package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentConfirmRecoveryTransactionService {

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final PaymentConfirmAttemptResolver attemptResolver;
    private final Clock clock;

    PaymentConfirmRecoveryTransactionService(PaymentAttemptReaderPort attemptReader,
                                             PaymentAttemptStorePort attemptStore,
                                             PaymentConfirmAttemptResolver attemptResolver,
                                             Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptStore = attemptStore;
        this.attemptResolver = attemptResolver;
        this.clock = clock;
    }

    /**
     * 배치가 저장된 결제 정보만으로 동일 confirm 요청을 복원한다.
     *
     * <p>후보 목록 조회 이후의 상태 변경을 반영하도록 행 잠금 아래 상태와 제한 시간을 다시 확인한다.
     * 반환 후 경합은 실제 confirm의 실행권 선점과 멱등성 검증이 처리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConfirmRecoveryStep resolveConfirmRecovery(Long attemptId) {
        PaymentAttempt attempt = findForUpdate(attemptId);
        Instant nowInstant = clock.instant();
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, clock.getZone());
        LocalDateTime activityStaleBefore =
                now.minus(PaymentConfirmClaimTransactionService.CONFIRM_RECOVERY_DELAY);
        LocalDateTime createdAtStaleBeforeUtc = LocalDateTime.ofInstant(
                nowInstant.minus(PaymentConfirmClaimTransactionService.CONFIRM_RECOVERY_DELAY), ZoneOffset.UTC);
        if (!attempt.isConfirmRecoveryCandidate(activityStaleBefore, createdAtStaleBeforeUtc)) {
            return new RecoverySkipped();
        }
        attempt.markConfirmRecoveryAttempted(now);
        if (attempt.requiresConfirmReconciliation(LocalDateTime.ofInstant(
                nowInstant.minus(PaymentConfirmClaimTransactionService.CONFIRM_AUTOMATIC_RETRY_MAX_AGE),
                ZoneOffset.UTC))) {
            attempt.markConfirmReconciliationRequired(
                    PaymentConfirmClaimTransactionService.CONFIRM_RECONCILIATION_REASON);
            attemptStore.save(attempt);
            return new ReconciliationRequired();
        }
        attemptStore.save(attempt);
        try {
            PaymentPayload payload = attemptResolver.readPayload(attempt);
            AuthContext auth = payload.userId() == null
                    ? AuthContext.guest()
                    : AuthContext.member(payload.userId());
            return new RecoveryReady(ConfirmCommand.trustedRecovery(
                    attempt.getPaymentKey(), attempt.getOrderIdExternal(), attempt.getAmount(), auth));
        } catch (RuntimeException failure) {
            return new RecoveryPreparationFailed(failure);
        }
    }

    private PaymentAttempt findForUpdate(Long attemptId) {
        return attemptReader.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new NotFoundException("결제 시도"));
    }

    sealed interface ConfirmRecoveryStep
            permits RecoverySkipped, ReconciliationRequired, RecoveryReady, RecoveryPreparationFailed {}

    record RecoverySkipped() implements ConfirmRecoveryStep {}

    record ReconciliationRequired() implements ConfirmRecoveryStep {}

    record RecoveryReady(ConfirmCommand command) implements ConfirmRecoveryStep {}

    record RecoveryPreparationFailed(RuntimeException failure) implements ConfirmRecoveryStep {}
}
