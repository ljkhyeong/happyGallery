package com.personal.happygallery.policy;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositCalculator;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("policy")
class BookingDepositPolicyTest {

    @DisplayName("일반 예약은 최소 1원의 예약금이 계산되는 클래스 가격만 허용한다")
    @Test
    void bookingClassPrice_keepsDepositPositive() {
        assertThatThrownBy(() -> new BookingClass(
                "가격 미달 클래스", "CRAFT", 60, BookingClass.MIN_PRICE - 1, 30))
                .isInstanceOf(HappyGalleryException.class);

        BookingClass bookingClass = new BookingClass(
                "최소 가격 클래스", "CRAFT", 60, BookingClass.MIN_PRICE, 30);
        Slot slot = new Slot(bookingClass, LocalDateTime.of(2030, 1, 1, 10, 0));

        assertThat(DepositCalculator.calculate(slot, 1))
                .satisfies(amounts -> {
                    assertThat(amounts.depositAmount()).isEqualTo(1L);
                    assertThat(amounts.balanceAmount()).isEqualTo(9L);
                });
    }
}
