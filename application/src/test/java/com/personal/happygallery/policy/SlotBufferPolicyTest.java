package com.personal.happygallery.policy;

import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [PolicyTest] 뒤쪽 버퍼 윈도우 경계 검증.
 *
 * <p>차단 대상 범위: {@code [endAt, endAt + bufferMin)} — 시작 포함, 끝 미포함.
 */
@Tag("policy")
class SlotBufferPolicyTest {

    private static final LocalDateTime END_AT = LocalDateTime.of(2026, 3, 1, 12, 0);
    private static final int BUFFER_MIN = 30;

    @DisplayName("버퍼 종료 시각은 종료 시각에 버퍼 분을 더한 값이다")
    @Test
    void bufferWindowEnd_isEndAtPlusBufferMin() {
        assertThat(SlotBufferPolicy.bufferWindowEnd(END_AT, BUFFER_MIN))
                .isEqualTo(END_AT.plusMinutes(BUFFER_MIN));
    }

    @DisplayName("앞 회차와 뒤 회차 모두 수업 시간과 정리 시간이 겹치면 충돌한다")
    @Test
    void conflicts_checksBothDirections() {
        LocalDateTime firstStart = LocalDateTime.of(2026, 3, 1, 10, 0);
        LocalDateTime firstEnd = LocalDateTime.of(2026, 3, 1, 12, 0);

        assertThat(SlotBufferPolicy.conflicts(
                firstStart, firstEnd,
                LocalDateTime.of(2026, 3, 1, 9, 0),
                LocalDateTime.of(2026, 3, 1, 9, 45),
                BUFFER_MIN)).isTrue();
        assertThat(SlotBufferPolicy.conflicts(
                firstStart, firstEnd,
                LocalDateTime.of(2026, 3, 1, 12, 30),
                LocalDateTime.of(2026, 3, 1, 13, 30),
                BUFFER_MIN)).isFalse();
    }
}
