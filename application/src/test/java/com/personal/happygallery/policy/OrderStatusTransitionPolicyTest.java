package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.AlreadyRefundedException;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.ProductionRefundNotAllowedException;
import com.personal.happygallery.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [PolicyTest] 주문 상태 전이 가드 검증.
 *
 * <p>이미 환불된 주문({@code REJECTED}, {@code AUTO_REFUND_TIMEOUT},
 * {@code PICKUP_EXPIRED}, {@code DELAY_REJECTED_CANCELED})에 승인을 시도하면
 * {@link AlreadyRefundedException}이 발생한다.
 */
@Tag("policy")
class OrderStatusTransitionPolicyTest {

    @DisplayName("승인 가능 검증은 대기 상태만 허용하고 환불성 상태를 거절한다")
    @Test
    void requireApprovable_validatesApprovalPolicy() {
        assertSoftly(softly -> {
            softly.assertThatCode(() -> OrderStatus.PAID_APPROVAL_PENDING.requireApprovable())
                    .as("PAID_APPROVAL_PENDING은 승인 가능")
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() -> OrderStatus.AUTO_REFUND_TIMEOUT.requireApprovable())
                    .as("AUTO_REFUND_TIMEOUT은 이미 환불된 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.REJECTED.requireApprovable())
                    .as("REJECTED는 이미 환불된 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.PICKUP_EXPIRED.requireApprovable())
                    .as("PICKUP_EXPIRED는 이미 환불된 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.DELAY_REJECTED_CANCELED.requireApprovable())
                    .as("DELAY_REJECTED_CANCELED는 이미 환불된 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
        });
    }

    // -----------------------------------------------------------------------
    // requireDelayRequested() — 제작 재개 가드
    // -----------------------------------------------------------------------

    @DisplayName("제작 재개 검증은 지연 요청 상태만 허용한다")
    @Test
    void requireDelayRequested_validatesResumePolicy() {
        assertSoftly(softly -> {
            softly.assertThatCode(() -> OrderStatus.DELAY_REQUESTED.requireDelayRequested())
                    .as("DELAY_REQUESTED는 제작 재개 가능")
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() -> OrderStatus.IN_PRODUCTION.requireDelayRequested())
                    .as("IN_PRODUCTION은 지연 요청 상태가 아님")
                    .isInstanceOf(HappyGalleryException.class);
        });
    }

    // -----------------------------------------------------------------------
    // requireDelayRejectionCancelable() — 고객 지연 거절 취소 가드
    // -----------------------------------------------------------------------

    @DisplayName("지연 거절 취소 검증은 제작 중 상태만 허용한다")
    @Test
    void requireDelayRejectionCancelable_validatesDelayRejectionPolicy() {
        assertSoftly(softly -> {
            softly.assertThatCode(() -> OrderStatus.IN_PRODUCTION.requireDelayRejectionCancelable())
                    .as("IN_PRODUCTION은 지연 거절 취소 가능")
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() -> OrderStatus.DELAY_REQUESTED.requireDelayRejectionCancelable())
                    .as("DELAY_REQUESTED는 이미 고객 동의 대기 상태")
                    .isInstanceOf(HappyGalleryException.class);
        });
    }

    // -----------------------------------------------------------------------
    // requireCancellable() — 제작 중 취소 불가 (§8.3)
    // -----------------------------------------------------------------------

    @DisplayName("취소 가능 검증은 제작 중 상태를 거절하고 대기 상태를 허용한다")
    @Test
    void requireCancellable_validatesProductionCancelPolicy() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> OrderStatus.IN_PRODUCTION.requireCancellable())
                    .as("IN_PRODUCTION은 제작 중 환불 불가")
                    .isInstanceOf(ProductionRefundNotAllowedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.DELAY_REQUESTED.requireCancellable())
                    .as("DELAY_REQUESTED는 제작 중 환불 불가")
                    .isInstanceOf(ProductionRefundNotAllowedException.class);
            softly.assertThatCode(() -> OrderStatus.PAID_APPROVAL_PENDING.requireCancellable())
                    .as("PAID_APPROVAL_PENDING은 취소 가능")
                    .doesNotThrowAnyException();
            softly.assertThatCode(() -> OrderStatus.APPROVED_FULFILLMENT_PENDING.requireCancellable())
                    .as("APPROVED_FULFILLMENT_PENDING은 취소 가능")
                    .doesNotThrowAnyException();
        });
    }
}
