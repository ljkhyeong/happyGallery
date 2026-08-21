package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderClaimItem;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderClaimRefundCalculatorTest {

    @DisplayName("클레임 고객 반환액은 품목의 PG 결제액과 적립금 사용액 비율로 분해한다")
    @Test
    void allocateRefundAmount_splitsPgAndRewardProportionally() {
        Order order = order(6_000L, 3_000L);
        OrderClaimItem claimItem = new OrderClaimItem(1L, 10L, 100L, 1);
        OrderItem orderItem = item(100L, 2, 6_000L, 4_000L);
        OrderClaimLine line = new OrderClaimLine(claimItem, orderItem);

        var allocation = OrderClaimRefundCalculator.allocateRefundAmount(
                order, List.of(line), 2_500L, 0L, 60L, 0L);

        assertSoftly(softly -> {
            softly.assertThat(allocation.pgRefundAmount()).isEqualTo(1_500L);
            softly.assertThat(allocation.rewardRestoreAmount()).isEqualTo(1_000L);
            softly.assertThat(allocation.rewardRevokeAmount()).isEqualTo(15L);
            softly.assertThat(claimItem.getApprovedRefundAmount()).isEqualTo(2_500L);
            softly.assertThat(claimItem.getApprovedRewardRestoreAmount()).isEqualTo(1_000L);
        });
    }

    @DisplayName("같은 품목을 여러 번 부분 환불하면 마지막 수량이 원 단위 나머지를 이어받는다")
    @Test
    void claimLine_sequentialQuantityPreservesComponentTotal() {
        OrderItem orderItem = item(100L, 2, 5L, 1L);
        OrderClaimLine first = new OrderClaimLine(
                new OrderClaimItem(1L, 10L, 100L, 1), orderItem, 0L);
        OrderClaimLine second = new OrderClaimLine(
                new OrderClaimItem(2L, 10L, 100L, 1), orderItem, 1L);

        assertSoftly(softly -> {
            softly.assertThat(first.pgRefundableAmount()).isEqualTo(2L);
            softly.assertThat(first.rewardRestorableAmount()).isZero();
            softly.assertThat(second.pgRefundableAmount()).isEqualTo(3L);
            softly.assertThat(second.rewardRestorableAmount()).isEqualTo(1L);
            softly.assertThat(first.customerRefundableAmount()
                    + second.customerRefundableAmount()).isEqualTo(6L);
        });
    }

    @DisplayName("전 품목 클레임의 배송비 환불액은 적립금이 아닌 PG 취소액에만 더한다")
    @Test
    void allocateRefundAmount_keepsShippingEntirelyInPg() {
        Order order = order(6_000L, 3_000L);
        OrderClaimItem claimItem = new OrderClaimItem(1L, 10L, 100L, 2);
        OrderClaimLine line = new OrderClaimLine(
                claimItem, item(100L, 2, 6_000L, 4_000L));

        var allocation = OrderClaimRefundCalculator.allocateRefundAmount(
                order, List.of(line), 13_000L, 0L, 60L, 0L);

        assertSoftly(softly -> {
            softly.assertThat(allocation.shippingRefundAmount()).isEqualTo(3_000L);
            softly.assertThat(allocation.pgRefundAmount()).isEqualTo(9_000L);
            softly.assertThat(allocation.rewardRestoreAmount()).isEqualTo(4_000L);
            softly.assertThat(claimItem.getApprovedRefundAmount()).isEqualTo(10_000L);
        });
    }

    private Order order(long rewardEarnBase, long shippingFee) {
        Order order = mock(Order.class);
        when(order.getRewardEarnBase()).thenReturn(rewardEarnBase);
        when(order.getShippingFee()).thenReturn(shippingFee);
        return order;
    }

    private OrderItem item(
            long id, int quantity, long netPaidAmount, long rewardUsedAmount) {
        OrderItem item = mock(OrderItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getQty()).thenReturn(quantity);
        when(item.getNetPaidAmount()).thenReturn(netPaidAmount);
        when(item.getRewardUsedAmount()).thenReturn(rewardUsedAmount);
        return item;
    }
}
