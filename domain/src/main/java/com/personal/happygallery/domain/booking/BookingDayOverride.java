package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** 공휴일 또는 특정 날짜를 기본 정책과 다르게 열거나 닫는다. */
@Entity
@Table(name = "booking_day_overrides")
public class BookingDayOverride {

    public static final int MAX_REASON_LENGTH = 200;

    @Id
    @Column(name = "calendar_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BookingDayAvailability availability;

    @Column(length = MAX_REASON_LENGTH)
    private String reason;

    protected BookingDayOverride() {}

    public BookingDayOverride(LocalDate date,
                              BookingDayAvailability availability,
                              String reason) {
        if (date == null || availability == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "날짜와 예약 상태는 필수입니다.");
        }
        this.date = date;
        this.availability = availability;
        this.reason = optionalReason(reason);
    }

    private static String optionalReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String normalized = reason.strip();
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약 제한 사유는 200자 이하여야 합니다.");
        }
        return normalized;
    }

    public LocalDate getDate() { return date; }
    public BookingDayAvailability getAvailability() { return availability; }
    public String getReason() { return reason; }
}
