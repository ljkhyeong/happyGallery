package com.personal.happygallery.application.order;

import com.personal.happygallery.application.coupon.port.out.CouponDefinitionReaderPort;
import com.personal.happygallery.application.coupon.port.out.IssuedCouponReaderPort;
import com.personal.happygallery.application.customer.port.out.MemberHistoryReaderPort;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingEventPort;
import com.personal.happygallery.application.payment.PaymentReceiptQuery;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.coupon.IssuedCouponStatus;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultOrderQueryServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final LocalDateTime now = LocalDateTime.now(clock);
    private final OrderReaderPort orders = mock(OrderReaderPort.class);
    private final IssuedCouponReaderPort coupons = mock(IssuedCouponReaderPort.class);
    private final CouponDefinitionReaderPort definitions = mock(CouponDefinitionReaderPort.class);
    private final Order order = mock(Order.class);
    private final DefaultOrderQueryService service = new DefaultOrderQueryService(
            orders, mock(OrderItemPort.class), mock(FulfillmentPort.class), mock(GuestTokenService.class),
            mock(RefundPort.class), mock(ShippingAddressProtector.class), mock(ShipmentTrackingEventPort.class),
            mock(PaymentReceiptQuery.class), mock(MemberHistoryReaderPort.class), coupons, definitions, clock);

    @BeforeEach
    void setUp() {
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        when(order.getId()).thenReturn(10L);
        when(order.getUserId()).thenReturn(1L);
    }

    @Test
    @DisplayName("복원된 쿠폰은 유효기간 직전까지 사용 가능하고 만료 시각부터 만료로 조회한다")
    void reflectsExpirationWithoutChangingStoredState() {
        IssuedCoupon coupon = availableCoupon();
        CouponDefinition definition = mock(CouponDefinition.class);
        when(definitions.findById(3L)).thenReturn(Optional.of(definition));
        when(definition.isActive()).thenReturn(true);
        when(definition.getValidUntil()).thenReturn(now.plusNanos(1), now);

        assertThat(service.findMyOrder(10L, 1L).couponStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
        assertThat(service.findMyOrder(10L, 1L).couponStatus()).isEqualTo(IssuedCouponStatus.EXPIRED);
        assertThat(coupon.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
    }

    @Test
    @DisplayName("사용 중지된 쿠폰은 복원됐더라도 사용 가능으로 안내하지 않는다")
    void reflectsDisabledDefinition() {
        availableCoupon();
        when(definitions.findById(3L)).thenReturn(Optional.of(mock(CouponDefinition.class)));
        assertThat(service.findMyOrder(10L, 1L).couponStatus()).isEqualTo(IssuedCouponStatus.CANCELED);
    }

    @ParameterizedTest
    @EnumSource(value = IssuedCouponStatus.class, names = {"RESERVED", "REDEEMED"})
    @DisplayName("다른 결제에서 다시 사용한 쿠폰은 현재 상태를 유지한다")
    void preservesReusedCouponStatus(IssuedCouponStatus status) {
        IssuedCoupon coupon = availableCoupon();
        coupon.reserve(20L, now);
        if (status == IssuedCouponStatus.REDEEMED) coupon.redeem(20L, 30L, now);
        assertThat(service.findMyOrder(10L, 1L).couponStatus()).isEqualTo(status);
        verifyNoInteractions(definitions);
    }

    @Test
    @DisplayName("쿠폰을 쓰지 않은 주문은 쿠폰 상태를 조회하지 않는다")
    void omitsUnusedCoupon() {
        when(order.getIssuedCouponId()).thenReturn(null);
        assertThat(service.findMyOrder(10L, 1L).couponStatus()).isNull();
        verifyNoInteractions(coupons, definitions);
    }

    @Test
    @DisplayName("다른 회원의 주문은 쿠폰 조회 전에 거절한다")
    void rejectsOtherOwnerBeforeCouponLookup() {
        assertThatThrownBy(() -> service.findMyOrder(10L, 2L)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(coupons, definitions);
    }

    private IssuedCoupon availableCoupon() {
        IssuedCoupon coupon = new IssuedCoupon(3L, 1L, now.minusDays(1));
        when(order.getIssuedCouponId()).thenReturn(2L);
        when(coupons.findById(2L)).thenReturn(Optional.of(coupon));
        return coupon;
    }
}
