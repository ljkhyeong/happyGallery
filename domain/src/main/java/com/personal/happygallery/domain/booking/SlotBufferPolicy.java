package com.personal.happygallery.domain.booking;

import java.time.LocalDateTime;

/**
 * 뒤쪽 버퍼 정책.
 *
 * <p>예약이 확정되면 슬롯 종료 시각 이후 buffer_min 분 동안 시작하는 슬롯을 비활성화한다.
 * 비활성화 대상 범위: {@code [endAt, endAt + bufferMin)} — 시작 포함, 끝 미포함.
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
}
