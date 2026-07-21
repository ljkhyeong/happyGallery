package com.personal.happygallery.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.personal.happygallery.domain.pass.PassPlan;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PassRefundAmountPolicyTest {

    @DisplayName("8회권 전액 환불은 원결제액의 원 단위까지 모두 반환한다")
    @Test
    void fullRefund_returnsOriginalPaymentAmount() {
        LocalDateTime purchasedAt = LocalDateTime.of(2026, 7, 21, 10, 0);
        PassPurchase pass = PassPurchase.forMember(
                1L, purchasedAt, purchasedAt.plusMonths(3), 240_003L, PassPlan.REGULAR_CRAFT_8);

        assertThat(pass.calculateRefundAmount(PassPurchase.TOTAL_CREDITS)).isEqualTo(240_003L);
    }
}
