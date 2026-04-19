package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.CapacityExceededException;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotCapacity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [PolicyTest] Slot 도메인 메서드 검증.
 *
 * <p>슬롯 생성 시 isActive=true, deactivate() 호출 후 false.
 * <p>incrementBookedCount() 정원 경계 검증.
 */
@Tag("policy")
class SlotDomainPolicyTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 3, 1, 10, 0);
    private static final LocalDateTime END   = LocalDateTime.of(2026, 3, 1, 12, 0);

    private Slot newSlot() {
        BookingClass cls = new BookingClass("향수 클래스", "PERFUME", 120, 50_000L, 30);
        return new Slot(cls, START, END);
    }

    @DisplayName("새 슬롯은 기본값으로 활성 상태다")
    @Test
    void newSlot_isActive_byDefault() {
        assertThat(newSlot().isActive()).isTrue();
    }

    @DisplayName("슬롯 비활성화 시 isActive가 false가 된다")
    @Test
    void deactivate_setsIsActiveFalse() {
        Slot slot = newSlot();
        slot.deactivate();
        assertThat(slot.isActive()).isFalse();
    }

    @DisplayName("슬롯 비활성화는 멱등하게 동작한다")
    @Test
    void deactivate_isIdempotent() {
        Slot slot = newSlot();
        slot.deactivate();
        slot.deactivate();
        assertThat(slot.isActive()).isFalse();
    }

    // --- incrementBookedCount ---

    @DisplayName("슬롯 예약 인원 증가는 정원 미만에서 성공한다")
    @Test
    void incrementBookedCount_underCapacity_succeeds() {
        Slot slot = newSlot();
        // MAX-1 번 증가 후 한 번 더 — 정원(MAX=8)에 딱 맞아야 성공
        for (int i = 0; i < SlotCapacity.MAX - 1; i++) {
            slot.incrementBookedCount();
        }
        assertThatCode(slot::incrementBookedCount).doesNotThrowAnyException();
        assertThat(slot.getBookedCount()).isEqualTo(SlotCapacity.MAX);
    }

    @DisplayName("슬롯 예약 인원 증가 시 정원에 도달하면 예외가 발생한다")
    @Test
    void incrementBookedCount_atCapacity_throws() {
        Slot slot = newSlot();
        for (int i = 0; i < SlotCapacity.MAX; i++) {
            slot.incrementBookedCount();
        }
        assertThatThrownBy(slot::incrementBookedCount)
                .isInstanceOf(CapacityExceededException.class);
        assertThat(slot.getBookedCount()).isEqualTo(SlotCapacity.MAX); // 롤백 미발생 케이스 — count 불변 확인
    }
}
