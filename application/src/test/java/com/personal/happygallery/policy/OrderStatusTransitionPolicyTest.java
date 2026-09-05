package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.AlreadyRefundedException;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.ProductionRefundNotAllowedException;
import com.personal.happygallery.domain.order.OrderStatus;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [PolicyTest] 주문 상태 전이 가드 검증.
 *
 * <p>이미 환불된 주문({@code REJECTED}, {@code AUTO_REFUND_TIMEOUT},
 * {@code CUSTOMER_CANCELED}, {@code PICKUP_EXPIRED}, {@code DELAY_REJECTED_CANCELED})에 승인을 시도하면
 * {@link AlreadyRefundedException}이 발생한다.
 */
@Tag("policy")
class OrderStatusTransitionPolicyTest {

    @DisplayName("배송지는 배송 준비 전의 진행 중 주문에서만 수정할 수 있다")
    @Test
    void requireShippingAddressWritable_rejectsPreparedAndFinishedOrders() {
        assertSoftly(softly -> {
            for (OrderStatus status : OrderStatus.values()) {
                if (Set.of(OrderStatus.PAID_APPROVAL_PENDING, OrderStatus.APPROVED_FULFILLMENT_PENDING,
                        OrderStatus.IN_PRODUCTION, OrderStatus.DELAY_CONSENT_PENDING,
                        OrderStatus.DELAY_ACCEPTED).contains(status)) {
                    softly.assertThatCode(status::requireShippingAddressWritable).doesNotThrowAnyException();
                } else {
                    softly.assertThatThrownBy(status::requireShippingAddressWritable)
                            .isInstanceOf(HappyGalleryException.class);
                }
            }
        });
    }

    @DisplayName("승인 대기 검증은 환불 상태와 다른 종결 상태를 구분한다")
    @Test
    void requireApprovalPending_distinguishesRefundedAndOtherFinalStatuses() {
        assertSoftly(softly -> {
            softly.assertThatCode(() -> OrderStatus.PAID_APPROVAL_PENDING.requireApprovalPending())
                    .as("PAID_APPROVAL_PENDING은 승인 가능")
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() -> OrderStatus.AUTO_REFUND_TIMEOUT.requireApprovalPending())
                    .as("AUTO_REFUND_TIMEOUT은 이미 환불된 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.REJECTED.requireApprovalPending())
                    .as("REJECTED는 이미 환불된 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.CUSTOMER_CANCELED.requireApprovalPending())
                    .as("CUSTOMER_CANCELED는 이미 환불 요청된 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.DELAY_REJECTED_CANCELED.requireApprovalPending())
                    .as("DELAY_REJECTED_CANCELED는 이미 환불된 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.PICKUP_EXPIRED.requireApprovalPending())
                    .as("PICKUP_EXPIRED는 환불된 미수령 주문")
                    .isInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.PICKUP_FORFEITED.requireApprovalPending())
                    .as("PICKUP_FORFEITED는 환불되지 않은 미수령 종결 상태")
                    .isInstanceOf(HappyGalleryException.class)
                    .isNotInstanceOf(AlreadyRefundedException.class);
        });
    }

    // -----------------------------------------------------------------------
    // requireDelayAccepted() — 지연 후 처리 재개 가드
    // -----------------------------------------------------------------------

    @DisplayName("지연 후 처리 재개 검증은 지연 수락 상태만 허용한다")
    @Test
    void requireDelayAccepted_validatesResumePolicy() {
        assertSoftly(softly -> {
            softly.assertThatCode(() -> OrderStatus.DELAY_ACCEPTED.requireDelayAccepted())
                    .as("DELAY_ACCEPTED는 지연 후 처리 재개 가능")
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() -> OrderStatus.IN_PRODUCTION.requireDelayAccepted())
                    .as("IN_PRODUCTION은 지연 수락 상태가 아님")
                    .isInstanceOf(HappyGalleryException.class);
        });
    }

    @DisplayName("지연 제안 검증은 기성품 승인 대기와 주문제작 제작 중 상태를 구분한다")
    @Test
    void requireDelayProposable_distinguishesOrderItemType() {
        assertSoftly(softly -> {
            softly.assertThatCode(() ->
                            OrderStatus.PAID_APPROVAL_PENDING.requireReadyStockDelayProposable())
                    .as("기성품은 승인 전에 지연 제안 가능")
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() ->
                            OrderStatus.IN_PRODUCTION.requireReadyStockDelayProposable())
                    .as("기성품은 제작 중 상태로 지연 제안할 수 없음")
                    .isInstanceOf(HappyGalleryException.class);
            softly.assertThatCode(() ->
                            OrderStatus.IN_PRODUCTION.requireProductionDelayProposable())
                    .as("주문제작은 제작 중에 지연 제안 가능")
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() ->
                            OrderStatus.PAID_APPROVAL_PENDING.requireProductionDelayProposable())
                    .as("주문제작은 승인 전에 지연 제안할 수 없음")
                    .isInstanceOf(HappyGalleryException.class);
        });
    }

    // -----------------------------------------------------------------------
    // requireDelayRejectionCancelable() — 고객 지연 거절 취소 가드
    // -----------------------------------------------------------------------

    @DisplayName("지연 거절 취소 검증은 고객 응답 대기 상태만 허용한다")
    @Test
    void requireDelayRejectionCancelable_validatesDelayRejectionPolicy() {
        assertSoftly(softly -> {
            softly.assertThatCode(() -> OrderStatus.DELAY_CONSENT_PENDING.requireDelayRejectionCancelable())
                    .as("DELAY_CONSENT_PENDING은 지연 거절 취소 가능")
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() -> OrderStatus.IN_PRODUCTION.requireDelayRejectionCancelable())
                    .as("IN_PRODUCTION에는 아직 고객에게 지연을 제안하지 않음")
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
            softly.assertThatThrownBy(() -> OrderStatus.DELAY_ACCEPTED.requireCancellable())
                    .as("DELAY_ACCEPTED는 제작 중 환불 불가")
                    .isInstanceOf(ProductionRefundNotAllowedException.class);
            softly.assertThatThrownBy(() -> OrderStatus.DELAY_CONSENT_PENDING.requireCancellable())
                    .as("DELAY_CONSENT_PENDING은 일반 제작 취소 경로로 환불할 수 없음")
                    .isInstanceOf(ProductionRefundNotAllowedException.class);
            softly.assertThatCode(() -> OrderStatus.PAID_APPROVAL_PENDING.requireCancellable())
                    .as("PAID_APPROVAL_PENDING은 취소 가능")
                    .doesNotThrowAnyException();
            softly.assertThatCode(() -> OrderStatus.APPROVED_FULFILLMENT_PENDING.requireCancellable())
                    .as("APPROVED_FULFILLMENT_PENDING은 취소 가능")
                    .doesNotThrowAnyException();
        });
    }

    @DisplayName("고객 직접 취소는 승인 대기 주문만 허용한다")
    @Test
    void requireCustomerCancellationAllowed_onlyAllowsApprovalPending() {
        assertSoftly(softly -> {
            softly.assertThatCode(() ->
                            OrderStatus.PAID_APPROVAL_PENDING.requireCustomerCancellationAllowed())
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() ->
                            OrderStatus.APPROVED_FULFILLMENT_PENDING.requireCustomerCancellationAllowed())
                    .isInstanceOf(HappyGalleryException.class)
                    .isNotInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() ->
                            OrderStatus.CUSTOMER_CANCELED.requireCustomerCancellationAllowed())
                    .isInstanceOf(AlreadyRefundedException.class);
        });
    }

    @DisplayName("미수령 관리자 환불은 미수령 종료 상태에서만 허용한다")
    @Test
    void requireMissedPickupRefundable_onlyAllowsForfeitedPickup() {
        assertSoftly(softly -> {
            softly.assertThatCode(() ->
                            OrderStatus.PICKUP_FORFEITED.requireMissedPickupRefundable())
                    .doesNotThrowAnyException();
            softly.assertThatThrownBy(() ->
                            OrderStatus.PICKUP_READY.requireMissedPickupRefundable())
                    .isInstanceOf(HappyGalleryException.class)
                    .isNotInstanceOf(AlreadyRefundedException.class);
            softly.assertThatThrownBy(() ->
                            OrderStatus.PICKUP_EXPIRED.requireMissedPickupRefundable())
                    .isInstanceOf(AlreadyRefundedException.class);
        });
    }
}
