package com.personal.happygallery.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.pass.PassPlan;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("policy")
class PassRefundAmountPolicyTest {

    @DisplayName("8회권 전액 환불은 원결제액의 원 단위까지 모두 반환한다")
    @Test
    void fullRefund_returnsOriginalPaymentAmount() {
        LocalDateTime purchasedAt = LocalDateTime.of(2026, 7, 21, 10, 0);
        PassPurchase pass = PassPurchase.forMember(
                1L, purchasedAt, purchasedAt.plusMonths(3), 240_003L, PassPlan.REGULAR_CRAFT_8);

        assertThat(pass.calculateRefundAmount(PassPurchase.TOTAL_CREDITS)).isEqualTo(240_003L);
    }

    @DisplayName("회원 전용 8회권은 회원 식별자 없이 생성할 수 없다")
    @Test
    void forMember_nullUserId_rejected() {
        LocalDateTime purchasedAt = LocalDateTime.of(2026, 7, 21, 10, 0);

        assertThatThrownBy(() -> PassPurchase.forMember(
                null, purchasedAt, purchasedAt.plusMonths(3), 240_000L, PassPlan.REGULAR_CRAFT_8))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @DisplayName("환불 요청은 0원 이하 금액으로 생성할 수 없다")
    @Test
    void refund_nonPositiveAmount_rejected() {
        assertThatThrownBy(() -> Refund.forOrder(1L, 0L, "payment-key"))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> Refund.forOrder(1L, -1L, "payment-key"))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
