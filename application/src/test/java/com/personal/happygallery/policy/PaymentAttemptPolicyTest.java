package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class PaymentAttemptPolicyTest {

    @DisplayName("준비된 결제 시도는 선점과 PG 승인을 거쳐 확정된다")
    @Test
    void pendingAttempt_canBeConfirmed_whenAmountMatches() {
        PaymentAttempt attempt = PaymentAttempt.startForMember(
                "order-id", PaymentContext.ORDER, 10_000L, "{}", 1L);
        LocalDateTime processingAt = LocalDateTime.of(2026, 4, 23, 9, 59);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 4, 23, 10, 0);

        String processingToken = attempt.startProcessing(10_000L, "payment-key", processingAt);
        attempt.markApproved(processingToken, "confirmed-payment-key", approvedAt);
        attempt.markConfirmed(12L, "encrypted-access-token");

        assertSoftly(softly -> {
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(attempt.getPaymentKey()).isEqualTo("payment-key");
            softly.assertThat(attempt.getConfirmedPaymentKey()).isEqualTo("confirmed-payment-key");
            softly.assertThat(attempt.getFulfilledDomainId()).isEqualTo(12L);
            softly.assertThat(attempt.getFulfilledAccessTokenEnc()).isEqualTo("encrypted-access-token");
            softly.assertThat(attempt.getProcessingAt()).isEqualTo(processingAt);
            softly.assertThat(attempt.getConfirmedAt()).isEqualTo(approvedAt);
        });
    }

    @DisplayName("결제 선점은 금액이 다르면 상태를 변경하지 않는다")
    @Test
    void startProcessing_rejectsConfirm_whenAmountDiffers() {
        PaymentAttempt attempt = PaymentAttempt.startForMember(
                "order-id", PaymentContext.BOOKING, 10_000L, "{}", 1L);
        LocalDateTime processingAt = LocalDateTime.of(2026, 4, 23, 10, 0);

        assertThatThrownBy(() -> attempt.startProcessing(9_000L, "payment-key", processingAt))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertSoftly(softly -> {
                        softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                        softly.assertThat(exception.getMessage()).isEqualTo("결제 금액이 일치하지 않습니다.");
                    });
                });

        assertSoftly(softly -> {
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
            softly.assertThat(attempt.getPaymentKey()).isNull();
            softly.assertThat(attempt.getConfirmedPaymentKey()).isNull();
            softly.assertThat(attempt.getConfirmedAt()).isNull();
        });
    }

    @DisplayName("실패 처리된 결제 시도는 다시 확정할 수 없다")
    @Test
    void failedAttempt_rejectsConfirm() {
        PaymentAttempt attempt = PaymentAttempt.startForMember(
                "order-id", PaymentContext.PASS, 240_000L, "{}", 1L);
        String processingToken = attempt.startProcessing(
                240_000L, "payment-key", LocalDateTime.of(2026, 4, 23, 10, 0));
        attempt.markProcessingFailed(processingToken, "PG 거절");

        assertThatThrownBy(() -> attempt.startProcessing(
                240_000L, "payment-key", LocalDateTime.of(2026, 4, 23, 10, 1)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertSoftly(softly -> {
                        softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                        softly.assertThat(exception.getMessage()).isEqualTo("이미 처리된 결제입니다.");
                    });
                });
    }

    @DisplayName("재선점된 결제는 이전 실행권 토큰의 직접 승인과 실패 저장을 거부한다")
    @Test
    void restartedAttempt_ignoresResultsFromPreviousProcessingToken() {
        PaymentAttempt attempt = PaymentAttempt.startForMember(
                "order-id", PaymentContext.ORDER, 10_000L, "{}", 1L);
        String firstToken = attempt.startProcessing(
                10_000L, "payment-key", LocalDateTime.of(2026, 7, 19, 10, 0));
        String secondToken = attempt.restartProcessing(
                10_000L, "payment-key", LocalDateTime.of(2026, 7, 19, 10, 2));

        assertSoftly(softly -> {
            softly.assertThat(secondToken).isNotEqualTo(firstToken);
            softly.assertThat(attempt.markRetryable(firstToken, "늦게 도착한 실패")).isFalse();
            softly.assertThat(attempt.markApproved(
                    firstToken, "confirmed-payment-key", LocalDateTime.of(2026, 7, 19, 10, 3))).isFalse();
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
            softly.assertThat(attempt.getProcessingToken()).isEqualTo(secondToken);
        });

        assertThat(attempt.markApproved(
                secondToken, "confirmed-payment-key", LocalDateTime.of(2026, 7, 19, 10, 4))).isTrue();
        assertSoftly(softly -> {
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
            softly.assertThat(attempt.getProcessingToken()).isNull();
        });
    }

    @DisplayName("늦게 도착한 PG 성공은 새 실행권의 실패 상태를 승인으로 화해한다")
    @Test
    void reconcileLatePgApproval_overridesNewerLocalFailure() {
        PaymentAttempt retryable = PaymentAttempt.startForMember(
                "retryable-order", PaymentContext.ORDER, 10_000L, "{}", 1L);
        retryable.startProcessing(
                10_000L, "retryable-payment-key", LocalDateTime.of(2026, 7, 19, 10, 0));
        String retryableOwner = retryable.restartProcessing(
                10_000L, "retryable-payment-key", LocalDateTime.of(2026, 7, 19, 10, 2));
        retryable.markRetryable(retryableOwner, "새 실행권의 일시 실패");

        PaymentAttempt failed = PaymentAttempt.startForMember(
                "failed-order", PaymentContext.ORDER, 20_000L, "{}", 1L);
        failed.startProcessing(
                20_000L, "failed-payment-key", LocalDateTime.of(2026, 7, 19, 10, 0));
        String failedOwner = failed.restartProcessing(
                20_000L, "failed-payment-key", LocalDateTime.of(2026, 7, 19, 10, 2));
        failed.markProcessingFailed(failedOwner, "새 실행권의 최종 실패");

        assertSoftly(softly -> {
            softly.assertThat(retryable.reconcileLatePgApproval(
                    "retryable-confirmed-key", LocalDateTime.of(2026, 7, 19, 10, 3))).isTrue();
            softly.assertThat(retryable.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
            softly.assertThat(retryable.getConfirmedPaymentKey()).isEqualTo("retryable-confirmed-key");
            softly.assertThat(failed.reconcileLatePgApproval(
                    "failed-confirmed-key", LocalDateTime.of(2026, 7, 19, 10, 3))).isTrue();
            softly.assertThat(failed.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
            softly.assertThat(failed.getConfirmedPaymentKey()).isEqualTo("failed-confirmed-key");
        });
    }

    @DisplayName("결제 확정 복구 후보는 상태별 기준 시각이 제한 시간에 도달한 경우만 선택한다")
    @Test
    void confirmRecoveryCandidate_usesProcessingAndApprovalBoundaries() {
        LocalDateTime boundary = LocalDateTime.of(2026, 7, 19, 10, 0);
        PaymentAttempt staleProcessing = PaymentAttempt.startForMember(
                "stale-processing", PaymentContext.ORDER, 10_000L, "{}", 1L);
        staleProcessing.startProcessing(10_000L, "processing-key", boundary);
        PaymentAttempt freshProcessing = PaymentAttempt.startForMember(
                "fresh-processing", PaymentContext.ORDER, 10_000L, "{}", 1L);
        freshProcessing.startProcessing(10_000L, "processing-key", boundary.plusNanos(1));
        PaymentAttempt retryable = PaymentAttempt.startForMember(
                "retryable", PaymentContext.ORDER, 10_000L, "{}", 1L);
        String retryableToken = retryable.startProcessing(10_000L, "retryable-key", boundary);
        retryable.markRetryable(retryableToken, "PG 일시 실패");
        boolean retryableAtBoundary = retryable.isConfirmRecoveryCandidate(boundary, boundary);
        retryable.markConfirmRecoveryAttempted(boundary.plusNanos(1));

        PaymentAttempt staleApproved = PaymentAttempt.startForMember(
                "stale-approved", PaymentContext.ORDER, 10_000L, "{}", 1L);
        String staleToken = staleApproved.startProcessing(
                10_000L, "approved-key", boundary.minusMinutes(10));
        staleApproved.markApproved(staleToken, "confirmed-key", boundary);
        PaymentAttempt freshApproved = PaymentAttempt.startForMember(
                "fresh-approved", PaymentContext.ORDER, 10_000L, "{}", 1L);
        String freshToken = freshApproved.startProcessing(
                10_000L, "approved-key", boundary.minusMinutes(10));
        freshApproved.markApproved(freshToken, "confirmed-key", boundary.plusNanos(1));

        PaymentAttempt confirmed = PaymentAttempt.startForMember(
                "confirmed", PaymentContext.ORDER, 10_000L, "{}", 1L);
        String confirmedToken = confirmed.startProcessing(10_000L, "confirmed-key", boundary.minusMinutes(10));
        confirmed.markApproved(confirmedToken, "confirmed-key", boundary.minusMinutes(5));
        confirmed.markConfirmed(1L, null);
        PaymentAttempt zeroAmount = PaymentAttempt.startForMember(
                "zero-amount", PaymentContext.BOOKING, 0L, "{}", 1L);
        zeroAmount.startProcessing(0L, null, boundary.minusYears(1));

        assertSoftly(softly -> {
            softly.assertThat(staleProcessing.isConfirmRecoveryCandidate(boundary, boundary)).isTrue();
            softly.assertThat(freshProcessing.isConfirmRecoveryCandidate(boundary, boundary)).isFalse();
            softly.assertThat(retryableAtBoundary).isTrue();
            softly.assertThat(retryable.isConfirmRecoveryCandidate(boundary, boundary)).isFalse();
            softly.assertThat(staleApproved.isConfirmRecoveryCandidate(boundary, boundary)).isTrue();
            softly.assertThat(freshApproved.isConfirmRecoveryCandidate(boundary, boundary)).isFalse();
            softly.assertThat(confirmed.isConfirmRecoveryCandidate(boundary, boundary)).isFalse();
            softly.assertThat(zeroAmount.requiresConfirmReconciliation(boundary.minusDays(14))).isFalse();
        });
    }
}
