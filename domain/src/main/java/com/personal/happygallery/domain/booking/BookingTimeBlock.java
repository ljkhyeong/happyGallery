package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;

/** 특정 날짜에서 신규 예약을 받지 않는 시간 구간. */
@Entity
@Table(name = "booking_time_blocks")
public class BookingTimeBlock {

    public static final int MAX_REASON_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calendar_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = MAX_REASON_LENGTH)
    private String reason;

    protected BookingTimeBlock() {}

    public BookingTimeBlock(LocalDate date,
                            LocalTime startTime,
                            LocalTime endTime,
                            String reason) {
        if (date == null || startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "예약 차단 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = optionalReason(reason);
    }

    public boolean overlaps(LocalTime candidateStart, LocalTime candidateEnd) {
        return candidateStart.isBefore(endTime) && startTime.isBefore(candidateEnd);
    }

    private static String optionalReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String normalized = reason.strip();
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약 제한 사유는 200자 이하여야 합니다.");
        }
        return normalized;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getReason() { return reason; }
}
