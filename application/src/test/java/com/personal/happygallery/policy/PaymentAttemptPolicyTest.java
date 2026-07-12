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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class PaymentAttemptPolicyTest {

    @DisplayName("준비된 결제 시도는 선점과 PG 승인을 거쳐 확정된다")
    @Test
    void pendingAttempt_canBeConfirmed_whenAmountMatches() {
        PaymentAttempt attempt = PaymentAttempt.start("order-id", PaymentContext.ORDER, 10_000L, "{}");
        LocalDateTime processingAt = LocalDateTime.of(2026, 4, 23, 9, 59);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 4, 23, 10, 0);

        assertThatCode(() -> attempt.requireConfirmable(10_000L))
                .doesNotThrowAnyException();
        attempt.startProcessing(10_000L, "payment-key", processingAt);
        attempt.markApproved("pg-ref", approvedAt);
        attempt.markConfirmed();

        assertSoftly(softly -> {
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(attempt.getPaymentKey()).isEqualTo("payment-key");
            softly.assertThat(attempt.getPgRef()).isEqualTo("pg-ref");
            softly.assertThat(attempt.getProcessingAt()).isEqualTo(processingAt);
            softly.assertThat(attempt.getConfirmedAt()).isEqualTo(approvedAt);
        });
    }

    @DisplayName("준비된 결제 시도는 금액이 다르면 확정할 수 없다")
    @Test
    void pendingAttempt_rejectsConfirm_whenAmountDiffers() {
        PaymentAttempt attempt = PaymentAttempt.start("order-id", PaymentContext.BOOKING, 10_000L, "{}");

        assertThatThrownBy(() -> attempt.requireConfirmable(9_000L))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertSoftly(softly -> {
                        softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                        softly.assertThat(exception.getMessage()).isEqualTo("결제 금액이 일치하지 않습니다.");
                    });
                });
    }

    @DisplayName("결제 선점은 금액이 다르면 상태를 변경하지 않는다")
    @Test
    void markConfirmed_rejectsConfirm_whenAmountDiffers() {
        PaymentAttempt attempt = PaymentAttempt.start("order-id", PaymentContext.BOOKING, 10_000L, "{}");
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
            softly.assertThat(attempt.getPgRef()).isNull();
            softly.assertThat(attempt.getConfirmedAt()).isNull();
        });
    }

    @DisplayName("실패 처리된 결제 시도는 다시 확정할 수 없다")
    @Test
    void failedAttempt_rejectsConfirm() {
        PaymentAttempt attempt = PaymentAttempt.start("order-id", PaymentContext.PASS, 240_000L, "{}");
        attempt.startProcessing(240_000L, "payment-key", LocalDateTime.of(2026, 4, 23, 10, 0));
        attempt.markFailed("PG 거절");

        assertThatThrownBy(() -> attempt.requireConfirmable(240_000L))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertSoftly(softly -> {
                        softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                        softly.assertThat(exception.getMessage()).isEqualTo("이미 처리된 결제입니다.");
                    });
                });
    }
}
