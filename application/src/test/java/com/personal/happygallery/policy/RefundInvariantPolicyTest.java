package com.personal.happygallery.policy;

import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class RefundInvariantPolicyTest {

    @DisplayName("PG 환불 성공 결과는 환불 거래 키가 반드시 있어야 한다")
    @Test
    void successfulRefundResult_requiresTransactionKey() {
        assertThatThrownBy(() -> RefundResult.success(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RefundResult.success("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("환불 성공 상태 전이는 빈 환불 거래 키를 저장하지 않는다")
    @Test
    void refundSuccessTransition_rejectsBlankTransactionKey() {
        Refund refund = Refund.forOrder(1L, 10_000L, "payment-key");
        LocalDateTime now = LocalDateTime.of(2026, 8, 8, 10, 0);
        String processingToken = refund.startProcessing(now, now.minusMinutes(1));

        assertThatThrownBy(() -> refund.markSucceeded(processingToken, " ", now.plusSeconds(1)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertSoftly(softly -> {
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
            softly.assertThat(refund.getRefundTransactionKey()).isNull();
            softly.assertThat(refund.getSucceededAt()).isNull();
            softly.assertThat(refund.getProcessingToken()).isEqualTo(processingToken);
        });
    }

    @DisplayName("혼합 결제 환불은 PG 취소액과 적립금 복원액의 합계를 고객 반환액으로 고정한다")
    @Test
    void mixedRefund_snapshotsCustomerAndBenefitAmounts() {
        Refund refund = Refund.forOrderClaim(
                1L, 2L, 7_000L, 10_000L, 3_000L, 50L, "payment-key");

        assertSoftly(softly -> {
            softly.assertThat(refund.getAmount()).isEqualTo(7_000L);
            softly.assertThat(refund.getCustomerRefundAmount()).isEqualTo(10_000L);
            softly.assertThat(refund.getRewardRestoreAmount()).isEqualTo(3_000L);
            softly.assertThat(refund.getRewardRevokeAmount()).isEqualTo(50L);
            softly.assertThat(refund.isRestoreCoupon()).isFalse();
        });
    }

    @DisplayName("PG 취소액이 0원인 주문 환불은 거래 키 없이 로컬 성공할 수 있다")
    @Test
    void localOnlyRefund_succeedsWithoutPgTransactionKey() {
        Refund refund = Refund.forOrder(
                1L, 0L, 5_000L, 5_000L, 0L, true, null);
        LocalDateTime now = LocalDateTime.of(2026, 8, 8, 11, 0);
        String processingToken = refund.startProcessing(now, now.minusMinutes(1));

        boolean transitioned = refund.markLocallySucceeded(processingToken, now.plusSeconds(1));

        assertSoftly(softly -> {
            softly.assertThat(transitioned).isTrue();
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(refund.getRefundTransactionKey()).isNull();
            softly.assertThat(refund.getSucceededAt()).isEqualTo(now.plusSeconds(1));
        });
    }

    @DisplayName("고객 반환액이 PG 취소액과 적립금 복원액의 합계와 다르면 거부한다")
    @Test
    void mixedRefund_rejectsMismatchedCustomerAmount() {
        assertThatThrownBy(() -> Refund.forOrder(
                1L, 7_000L, 9_999L, 3_000L, 0L, false, "payment-key"))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
