package com.personal.happygallery.policy;

import com.personal.happygallery.application.coupon.port.in.CouponQuote;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.coupon.IssuedCouponStatus;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class CouponPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 8, 12, 0);

    @DisplayName("정액과 정률 할인은 상품 금액만 기준으로 계산하고 상품 금액을 넘지 않는다")
    @Test
    void discount_capsAtProductAmount() {
        CouponDefinition fixed = fixedCoupon(20_000L, 0L, NOW.plusDays(1));
        CouponDefinition percent = new CouponDefinition(
                "15% 할인",
                CouponDiscountType.PERCENT,
                15L,
                50_000L,
                10_000L,
                NOW.minusDays(1),
                NOW.plusDays(1),
                true,
                true);

        assertSoftly(softly -> {
            softly.assertThat(fixed.calculateDiscount(12_000L, NOW)).isEqualTo(12_000L);
            softly.assertThat(percent.calculateDiscount(100_000L, NOW)).isEqualTo(10_000L);
            softly.assertThat(new CouponQuote(1L, 2L, "쿠폰", 12_000L, 12_000L)
                            .discountedProductAmount())
                    .isZero();
        });
    }

    @DisplayName("정률 계산 결과가 1원 미만이면 쿠폰 정책 위반으로 거절한다")
    @Test
    void percentDiscount_rejectsZeroWonResult() {
        CouponDefinition percent = new CouponDefinition(
                "1% 할인",
                CouponDiscountType.PERCENT,
                1L,
                0L,
                10_000L,
                NOW.minusDays(1),
                NOW.plusDays(1),
                true,
                true);

        assertThatThrownBy(() -> percent.calculateDiscount(99L, NOW))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHANGE_NOT_ALLOWED);
                    assertThat(exception).hasMessageContaining("1원 이상");
                });
    }

    @DisplayName("정률 쿠폰은 1에서 100 사이 할인율과 최대 할인 금액을 필수로 가진다")
    @Test
    void percentCoupon_requiresRateAndMaximumDiscount() {
        assertThatThrownBy(() -> new CouponDefinition(
                "잘못된 정률 쿠폰",
                CouponDiscountType.PERCENT,
                101L,
                0L,
                10_000L,
                NOW.minusDays(1),
                NOW.plusDays(1),
                true,
                true))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("1 이상 100 이하");

        assertThatThrownBy(() -> new CouponDefinition(
                "상한 없는 정률 쿠폰",
                CouponDiscountType.PERCENT,
                10L,
                0L,
                null,
                NOW.minusDays(1),
                NOW.plusDays(1),
                true,
                true))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("최대 할인 금액은 필수");
    }

    @DisplayName("쿠폰 유효기간은 시작 시각을 포함하고 종료 시각을 포함하지 않는다")
    @Test
    void validity_usesExclusiveEndBoundary() {
        CouponDefinition definition = fixedCoupon(5_000L, 0L, NOW.plusHours(1));

        assertThat(definition.isWithinValidity(NOW.minusDays(1))).isTrue();
        assertThat(definition.isWithinValidity(NOW.plusHours(1))).isFalse();
        assertThatThrownBy(() -> definition.calculateDiscount(10_000L, NOW.plusHours(1)))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessage("사용할 수 없는 쿠폰입니다.");
    }

    @DisplayName("발급 쿠폰은 같은 결제 시도 예약과 사용 완료 재호출을 멱등 처리한다")
    @Test
    void issuedCoupon_reserveAndRedeemAreIdempotent() {
        IssuedCoupon issued = new IssuedCoupon(1L, 2L, NOW.minusMinutes(1));

        issued.reserve(10L, NOW);
        issued.reserve(10L, NOW.plusSeconds(1));
        issued.redeem(10L, 20L, NOW.plusMinutes(1));
        issued.redeem(10L, 20L, NOW.plusMinutes(2));

        assertSoftly(softly -> {
            softly.assertThat(issued.getStatus()).isEqualTo(IssuedCouponStatus.REDEEMED);
            softly.assertThat(issued.getPaymentAttemptId()).isEqualTo(10L);
            softly.assertThat(issued.getUsedOrderId()).isEqualTo(20L);
            softly.assertThat(issued.getReservedAt()).isEqualTo(NOW);
            softly.assertThat(issued.getUsedAt()).isEqualTo(NOW.plusMinutes(1));
        });
    }

    @DisplayName("결제 예약 해제 시 만료 경계가 지났으면 쿠폰을 만료 처리한다")
    @Test
    void release_afterValidityExpires_marksExpired() {
        IssuedCoupon issued = new IssuedCoupon(1L, 2L, NOW.minusMinutes(1));
        issued.reserve(10L, NOW);

        issued.release(10L, NOW.plusMinutes(5), NOW.plusMinutes(5));

        assertSoftly(softly -> {
            softly.assertThat(issued.getStatus()).isEqualTo(IssuedCouponStatus.EXPIRED);
            softly.assertThat(issued.getPaymentAttemptId()).isNull();
            softly.assertThat(issued.getReservedAt()).isNull();
        });
    }

    @DisplayName("전액 취소된 주문의 쿠폰은 만료 전에는 복원되고 만료 뒤에는 사용 이력을 보존한 채 만료된다")
    @Test
    void restoreAfterFullCancellation_onlyBeforeExpiration() {
        IssuedCoupon restorable = redeemedCoupon();
        IssuedCoupon expired = redeemedCoupon();

        restorable.restoreAfterFullCancellation(20L, NOW.plusDays(1), NOW.plusHours(1));
        expired.restoreAfterFullCancellation(20L, NOW.plusDays(1), NOW.plusDays(1));

        assertSoftly(softly -> {
            softly.assertThat(restorable.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
            softly.assertThat(restorable.getPaymentAttemptId()).isNull();
            softly.assertThat(restorable.getUsedOrderId()).isNull();
            softly.assertThat(expired.getStatus()).isEqualTo(IssuedCouponStatus.EXPIRED);
            softly.assertThat(expired.getUsedOrderId()).isEqualTo(20L);
        });
    }

    private static IssuedCoupon redeemedCoupon() {
        IssuedCoupon issued = new IssuedCoupon(1L, 2L, NOW.minusMinutes(1));
        issued.reserve(10L, NOW);
        issued.redeem(10L, 20L, NOW.plusMinutes(1));
        return issued;
    }

    private static CouponDefinition fixedCoupon(long discountValue,
                                                long minOrderAmount,
                                                LocalDateTime validUntil) {
        return new CouponDefinition(
                "정액 할인",
                CouponDiscountType.FIXED,
                discountValue,
                minOrderAmount,
                null,
                NOW.minusDays(1),
                validUntil,
                true,
                true);
    }
}
