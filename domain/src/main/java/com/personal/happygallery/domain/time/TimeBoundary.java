package com.personal.happygallery.domain.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * 시간 경계 판정 유틸.
 * 모든 메서드는 Clock을 매개변수로 받아 테스트에서 Clock.fixed()로 경계를 재현할 수 있게 한다.
 */
public final class TimeBoundary {

    private TimeBoundary() {}

    // ── D-1 환불 경계 ──────────────────────────────────────────────────────────

    /**
     * D-1 환불 가능 여부.
     * 체험일 전날 00:00 Asia/Seoul 이전이면 환불 가능.
     */
    public static boolean isRefundable(LocalDate experienceDate, Clock clock) {
        ZonedDateTime deadline = experienceDate.atStartOfDay(Clocks.SEOUL);
        return ZonedDateTime.now(clock).isBefore(deadline);
    }

    /** 슬롯 시작 시각(LocalDateTime) 기반 환불 가능 여부. 날짜 변환을 내부에서 처리한다. */
    public static boolean isRefundable(LocalDateTime slotStartAt, Clock clock) {
        return isRefundable(slotStartAt.toLocalDate(), clock);
    }

    // ── 당일 변경 경계 ─────────────────────────────────────────────────────────

    /**
     * 당일 변경 가능 여부.
     * 슬롯 시작 1시간 전 이전이면 변경 가능.
     */
    public static boolean isChangeable(ZonedDateTime slotStart, Clock clock) {
        ZonedDateTime deadline = slotStart.minusHours(1);
        return ZonedDateTime.now(clock).isBefore(deadline);
    }

    /** 슬롯 시작 시각(LocalDateTime) 기반 변경 가능 여부. Asia/Seoul 타임존 변환을 내부에서 처리한다. */
    public static boolean isChangeable(LocalDateTime slotStartAt, Clock clock) {
        return isChangeable(slotStartAt.atZone(Clocks.SEOUL), clock);
    }

    // ── 8회권 만료 ─────────────────────────────────────────────────────────────

    /**
     * 8회권 만료 시점. 결제일로부터 90일.
     */
    public static ZonedDateTime passExpiresAt(ZonedDateTime purchasedAt) {
        return purchasedAt.plusDays(90);
    }

    /** 8회권 만료 시점을 LocalDateTime으로 반환한다. */
    public static LocalDateTime passExpiresAtLocal(ZonedDateTime purchasedAt) {
        return passExpiresAt(purchasedAt).toLocalDateTime();
    }

    /**
     * 만료 알림 시점. 만료일 7일 전.
     */
    public static ZonedDateTime passNotificationAt(ZonedDateTime expiresAt) {
        return expiresAt.minusDays(7);
    }
}
