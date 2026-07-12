package com.personal.happygallery.domain.booking;

import java.time.LocalDateTime;

/**
 * 뒤쪽 버퍼 정책.
 *
 * <p>예약이 확정되면 슬롯 종료 시각 이후 buffer_min 분 동안 시작하는 슬롯을 차단한다.
 * 차단 대상 범위: {@code [endAt, endAt + bufferMin)} — 시작 포함, 끝 미포함.
 */
public final class SlotBufferPolicy {

    private SlotBufferPolicy() {}

    /** 버퍼 윈도우 시작 (inclusive) — 슬롯 종료 시각과 동일 */
    public static LocalDateTime bufferWindowStart(LocalDateTime endAt) {
        return endAt;
    }

    /** 버퍼 윈도우 끝 (exclusive) — 슬롯 종료 시각 + bufferMin 분 */
    public static LocalDateTime bufferWindowEnd(LocalDateTime endAt, int bufferMin) {
        return endAt.plusMinutes(bufferMin);
    }

    /** 후보 슬롯 시작 시각이 원인 슬롯의 뒤쪽 버퍼 범위에 포함되는지 확인한다. */
    public static boolean contains(LocalDateTime endAt, int bufferMin, LocalDateTime candidateStartAt) {
        return !candidateStartAt.isBefore(bufferWindowStart(endAt))
                && candidateStartAt.isBefore(bufferWindowEnd(endAt, bufferMin));
    }
}
