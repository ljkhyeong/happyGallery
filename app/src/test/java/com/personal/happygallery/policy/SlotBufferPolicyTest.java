package com.personal.happygallery.policy;

import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [PolicyTest] 뒤쪽 버퍼 윈도우 경계 검증.
 *
 * <p>비활성화 대상 범위: {@code [endAt, endAt + bufferMin)} — 시작 포함, 끝 미포함.
 */
@Tag("policy")
class SlotBufferPolicyTest {

    private static final LocalDateTime END_AT     = LocalDateTime.of(2026, 3, 1, 12, 0);
    private static final int           BUFFER_MIN = 30;

    @Test
    void bufferWindowStart_equalsEndAt() {
        assertThat(SlotBufferPolicy.bufferWindowStart(END_AT)).isEqualTo(END_AT);
    }

    @Test
    void bufferWindowEnd_isEndAtPlusBufferMin() {
        assertThat(SlotBufferPolicy.bufferWindowEnd(END_AT, BUFFER_MIN))
                .isEqualTo(END_AT.plusMinutes(BUFFER_MIN));
    }

    @Test
    void slotAtEndAt_isInsideWindow() {
        // start_at == end_at → 포함(inclusive)
        LocalDateTime candidateStart = END_AT;
        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(END_AT);
        LocalDateTime windowEnd   = SlotBufferPolicy.bufferWindowEnd(END_AT, BUFFER_MIN);

        assertThat(isInWindow(candidateStart, windowStart, windowEnd)).isTrue();
    }

    @Test
    void slotOneMinuteAfterEndAt_isInsideWindow() {
        LocalDateTime candidateStart = END_AT.plusMinutes(1);
        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(END_AT);
        LocalDateTime windowEnd   = SlotBufferPolicy.bufferWindowEnd(END_AT, BUFFER_MIN);

        assertThat(isInWindow(candidateStart, windowStart, windowEnd)).isTrue();
    }

    @Test
    void slotAtWindowEnd_isOutsideWindow() {
        // start_at == end_at + bufferMin → 미포함(exclusive)
        LocalDateTime candidateStart = END_AT.plusMinutes(BUFFER_MIN);
        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(END_AT);
        LocalDateTime windowEnd   = SlotBufferPolicy.bufferWindowEnd(END_AT, BUFFER_MIN);

        assertThat(isInWindow(candidateStart, windowStart, windowEnd)).isFalse();
    }

    @Test
    void slotBeforeEndAt_isOutsideWindow() {
        LocalDateTime candidateStart = END_AT.minusMinutes(1);
        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(END_AT);
        LocalDateTime windowEnd   = SlotBufferPolicy.bufferWindowEnd(END_AT, BUFFER_MIN);

        assertThat(isInWindow(candidateStart, windowStart, windowEnd)).isFalse();
    }

    /** Repository 쿼리 조건과 동일: start >= windowStart AND start < windowEnd */
    private boolean isInWindow(LocalDateTime start, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return !start.isBefore(windowStart) && start.isBefore(windowEnd);
    }
}
