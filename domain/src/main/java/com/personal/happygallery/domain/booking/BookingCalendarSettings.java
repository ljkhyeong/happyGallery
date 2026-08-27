package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalTime;

/** 자동으로 열리는 예약 캘린더의 단일 운영 설정. */
@Entity
@Table(name = "booking_calendar_settings")
public class BookingCalendarSettings {

    public static final long SINGLETON_ID = 1L;
    public static final int MIN_SLOT_INTERVAL_MIN = 10;
    public static final int MAX_SLOT_INTERVAL_MIN = 120;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "slot_interval_min", nullable = false)
    private int slotIntervalMin;

    @Column(name = "block_public_holidays", nullable = false)
    private boolean blockPublicHolidays;

    @Version
    @Column(nullable = false)
    private long version;

    protected BookingCalendarSettings() {}

    public BookingCalendarSettings(LocalTime openTime,
                                   LocalTime closeTime,
                                   int slotIntervalMin,
                                   boolean blockPublicHolidays) {
        update(openTime, closeTime, slotIntervalMin, blockPublicHolidays);
    }

    public void update(LocalTime openTime,
                       LocalTime closeTime,
                       int slotIntervalMin,
                       boolean blockPublicHolidays) {
        if (openTime == null || closeTime == null || !openTime.isBefore(closeTime)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "예약 운영 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
        if (slotIntervalMin < MIN_SLOT_INTERVAL_MIN || slotIntervalMin > MAX_SLOT_INTERVAL_MIN) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "예약 시작 간격은 10분에서 120분 사이여야 합니다.");
        }
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.slotIntervalMin = slotIntervalMin;
        this.blockPublicHolidays = blockPublicHolidays;
    }

    public Long getId() { return id; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public int getSlotIntervalMin() { return slotIntervalMin; }
    public boolean isBlockPublicHolidays() { return blockPublicHolidays; }
    public long getVersion() { return version; }
}
