package com.personal.happygallery.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.OrderItemPricing;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.order.ProportionalAmountAllocator;
import com.personal.happygallery.domain.reward.RewardAccount;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("policy")
class OrderBenefitPolicyTest {

    @Test
    @DisplayName("쿠폰과 적립금은 상품 금액만 차감하고 배송비는 PG 결제액에 남긴다")
    void pricingSnapshot_excludesShippingFromBenefits() {
        OrderPricingSnapshot pricing = new OrderPricingSnapshot(
                100_000L, 3_000L, 20_000L, 30_000L, 53_000L, 7L);

        assertSoftly(softly -> {
            softly.assertThat(pricing.totalAmount()).isEqualTo(83_000L);
            softly.assertThat(pricing.pgPaidAmount()).isEqualTo(53_000L);
            softly.assertThat(pricing.rewardEarnBase()).isEqualTo(50_000L);
        });
    }

    @Test
    @DisplayName("품목 혜택 배분은 최대 나머지 방식으로 원 단위 합계를 정확히 보존한다")
    void proportionalAllocation_preservesWonTotal() {
        assertThat(ProportionalAmountAllocator.allocate(
                101L, List.of(100L, 100L, 100L)))
                .containsExactly(34L, 34L, 33L);
        assertThat(ProportionalAmountAllocator.allocate(
                50L, List.of(0L, 100L, 300L)))
                .containsExactly(0L, 13L, 37L);
    }

    @Test
    @DisplayName("품목 할인과 적립금 합계가 상품 금액을 넘으면 거부한다")
    void itemPricing_rejectsOverDiscount() {
        assertThatThrownBy(() -> new OrderItemPricing(10_000L, 8_000L, 3_000L, 0L))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("혜택 배분");
    }

    @Test
    @DisplayName("품목 원가 계산은 음수 수량과 음수 단가가 곱셈으로 양수가 되어도 거부한다")
    void itemPricing_rejectsNegativeQuantityAndUnitPrice() {
        assertThatThrownBy(() -> OrderItemPricing.fullPrice(-1, -1L))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("수량");
    }

    @Test
    @DisplayName("적립금 회수 부족분은 부채로 남고 다음 적립액이 부채를 먼저 상환한다")
    void rewardAccount_repaymentPrecedesAvailableCredit() {
        RewardAccount account = RewardAccount.open(1L);
        account.credit(1_000L);
        account.reserve(700L);
        account.consume(700L);

        long debited = account.revoke(1_000L);
        long credited = account.credit(800L);

        assertSoftly(softly -> {
            softly.assertThat(debited).isEqualTo(300L);
            softly.assertThat(credited).isEqualTo(100L);
            softly.assertThat(account.getAvailableBalance()).isEqualTo(100L);
            softly.assertThat(account.getReservedBalance()).isZero();
            softly.assertThat(account.getDebtBalance()).isZero();
        });
    }

    @Test
    @DisplayName("적립금 예약 반환액은 사용 가능 잔액보다 기존 부채를 먼저 상환한다")
    void rewardAccount_releaseRepaysDebtBeforeAvailableBalance() {
        RewardAccount account = RewardAccount.open(1L);
        account.credit(100L);
        account.reserve(100L);
        account.revoke(100L);

        account.release(100L);
        long availableCredit = account.credit(100L);

        assertSoftly(softly -> {
            softly.assertThat(availableCredit).isZero();
            softly.assertThat(account.getAvailableBalance()).isZero();
            softly.assertThat(account.getReservedBalance()).isZero();
            softly.assertThat(account.getDebtBalance()).isZero();
        });
    }
}
