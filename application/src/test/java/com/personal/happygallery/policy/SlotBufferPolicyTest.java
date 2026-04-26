package com.personal.happygallery.policy;

import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    @DisplayName("버퍼 시작 시각은 수업 종료 시각과 같다")
    @Test
    void bufferWindowStart_equalsEndAt() {
        assertThat(SlotBufferPolicy.bufferWindowStart(END_AT)).isEqualTo(END_AT);
    }

    @DisplayName("버퍼 종료 시각은 종료 시각에 버퍼 분을 더한 값이다")
    @Test
    void bufferWindowEnd_isEndAtPlusBufferMin() {
        assertThat(SlotBufferPolicy.bufferWindowEnd(END_AT, BUFFER_MIN))
                .isEqualTo(END_AT.plusMinutes(BUFFER_MIN));
    }

    @DisplayName("슬롯 시작 시각에 따라 버퍼 구간 포함 여부가 달라진다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("bufferWindowMembershipCases")
    void bufferWindowMembership(String caseName, LocalDateTime candidateStart, boolean expected) {
        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(END_AT);
        LocalDateTime windowEnd = SlotBufferPolicy.bufferWindowEnd(END_AT, BUFFER_MIN);

        assertThat(isInWindow(candidateStart, windowStart, windowEnd)).isEqualTo(expected);
    }

    /** Repository 쿼리 조건과 동일: start >= windowStart AND start < windowEnd */
    private boolean isInWindow(LocalDateTime start, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return !start.isBefore(windowStart) && start.isBefore(windowEnd);
    }

    private static Stream<Arguments> bufferWindowMembershipCases() {
        return Stream.of(
                Arguments.of("슬롯 시작 시각이 종료 시각과 같으면 버퍼 구간에 포함된다", END_AT, true),
                Arguments.of("슬롯 시작 시각이 종료 1분 후면 버퍼 구간에 포함된다", END_AT.plusMinutes(1), true),
                Arguments.of("슬롯 시작 시각이 버퍼 종료 시각과 같으면 버퍼 구간에서 제외된다", END_AT.plusMinutes(BUFFER_MIN), false),
                Arguments.of("슬롯 시작 시각이 종료 시각보다 이르면 버퍼 구간에서 제외된다", END_AT.minusMinutes(1), false)
        );
    }
}
