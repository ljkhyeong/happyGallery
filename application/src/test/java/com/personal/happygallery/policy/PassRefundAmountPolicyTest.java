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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("policy")
class PassRefundAmountPolicyTest {

    @DisplayName("4회권과 기존 8회권은 구매 횟수만큼 사용하고 남은 횟수에 비례해 환불한다")
    @ParameterizedTest
    @CsvSource({"REGULAR_CRAFT_4, 4, 120003, 90002", "REGULAR_CRAFT_8, 8, 240003, 210002"})
    void purchasedCreditsAndRefund_followPurchasedPlan(
            PassPlan plan, int credits, long price, long refundAfterOneUse) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 11, 10, 0);
        PassPurchase pass = PassPurchase.forMember(1L, now, now.plusDays(90), price, plan);

        assertThat(pass.getTotalCredits()).isEqualTo(credits);
        assertThat(pass.getRemainingCredits()).isEqualTo(credits);
        assertThat(pass.calculateRefundAmount(credits)).isEqualTo(price);
        pass.useCredit(now);
        assertThat(pass.calculateRefundAmount(pass.getRemainingCredits())).isEqualTo(refundAfterOneUse);
        for (int used = 1; used < credits; used++) {
            pass.useCredit(now);
        }
        assertThatThrownBy(() -> pass.useCredit(now))
                .isInstanceOfSatisfying(HappyGalleryException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PASS_CREDIT_INSUFFICIENT));
    }

    @DisplayName("이용권 전액 환불은 원결제액의 원 단위까지 모두 반환한다")
    @Test
    void fullRefund_returnsOriginalPaymentAmount() {
        LocalDateTime purchasedAt = LocalDateTime.of(2026, 7, 21, 10, 0);
        PassPurchase pass = PassPurchase.forMember(
                1L, purchasedAt, purchasedAt.plusMonths(3), 240_003L, PassPlan.REGULAR_CRAFT_8);

        assertThat(pass.calculateRefundAmount(pass.getTotalCredits())).isEqualTo(240_003L);
    }

    @DisplayName("이용권 만료 결과는 아직 유효하면 비어 있고 재처리하면 소멸 수량 0을 유지한다")
    @Test
    void expirationResult_distinguishesValidFromAlreadyExpired() {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 10, 21, 0, 0);
        PassPurchase pass = PassPurchase.forMember(
                1L, expiresAt.minusMonths(3), expiresAt, 240_000L, PassPlan.REGULAR_CRAFT_8);

        assertThat(pass.expireIfReached(expiresAt.minusNanos(1))).isEmpty();
        assertThat(pass.expireIfReached(expiresAt)).hasValue(pass.getTotalCredits());
        assertThat(pass.expireIfReached(expiresAt.plusNanos(1))).hasValue(0);
    }

    @DisplayName("회원 전용 이용권은 회원 식별자 없이 생성할 수 없다")
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
