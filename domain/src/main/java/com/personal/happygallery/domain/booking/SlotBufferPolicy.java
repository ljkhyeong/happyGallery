package com.personal.happygallery.domain.booking;

import java.time.LocalDateTime;

/**
 * 수업 시간과 뒤쪽 정리 버퍼 충돌 정책.
 *
 * <p>예약이 확정되면 수업 시작부터 종료 후 buffer_min 분까지 겹치는 다른 시작 시각을 차단한다.
 */
public final class SlotBufferPolicy {

    private SlotBufferPolicy() {}

    /** 버퍼 윈도우 끝 (exclusive) — 슬롯 종료 시각 + bufferMin 분 */
    public static LocalDateTime bufferWindowEnd(LocalDateTime endAt, int bufferMin) {
        return endAt.plusMinutes(bufferMin);
    }

    /** 두 슬롯의 수업 시간과 뒤쪽 정리 버퍼가 서로 겹치는지 확인한다. */
    public static boolean conflicts(LocalDateTime firstStartAt,
                                    LocalDateTime firstEndAt,
                                    LocalDateTime secondStartAt,
                                    LocalDateTime secondEndAt,
                                    int bufferMin) {
        return firstStartAt.isBefore(bufferWindowEnd(secondEndAt, bufferMin))
                && secondStartAt.isBefore(bufferWindowEnd(firstEndAt, bufferMin));
    }
}
