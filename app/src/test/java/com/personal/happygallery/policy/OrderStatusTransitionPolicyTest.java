package com.personal.happygallery.policy;

import com.personal.happygallery.common.error.AlreadyRefundedException;
import com.personal.happygallery.domain.order.OrderStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [PolicyTest] 주문 상태 전이 가드 검증.
 *
 * <p>이미 환불된 주문({@code REJECTED_REFUNDED}, {@code AUTO_REFUNDED_TIMEOUT},
 * {@code PICKUP_EXPIRED_REFUNDED})에 승인을 시도하면 {@link AlreadyRefundedException}이 발생한다.
 */
@Tag("policy")
class OrderStatusTransitionPolicyTest {

    @Test
    void requireApprovable_whenPaidPending_noException() {
        assertThatCode(() -> OrderStatus.PAID_APPROVAL_PENDING.requireApprovable())
                .doesNotThrowAnyException();
    }

    @Test
    void requireApprovable_whenAutoRefunded_throws() {
        assertThatThrownBy(() -> OrderStatus.AUTO_REFUNDED_TIMEOUT.requireApprovable())
                .isInstanceOf(AlreadyRefundedException.class);
    }

    @Test
    void requireApprovable_whenRejectedRefunded_throws() {
        assertThatThrownBy(() -> OrderStatus.REJECTED_REFUNDED.requireApprovable())
                .isInstanceOf(AlreadyRefundedException.class);
    }

    @Test
    void requireApprovable_whenPickupExpiredRefunded_throws() {
        assertThatThrownBy(() -> OrderStatus.PICKUP_EXPIRED_REFUNDED.requireApprovable())
                .isInstanceOf(AlreadyRefundedException.class);
    }
}
