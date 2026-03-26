package com.personal.happygallery.infra.dashboard.adapter;

import com.personal.happygallery.app.dashboard.dto.SlotUtilization;
import com.personal.happygallery.app.dashboard.port.out.BookingStatsQueryPort;
import com.personal.happygallery.infra.dashboard.mapper.BookingStatsMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class MyBatisBookingStatsAdapter implements BookingStatsQueryPort {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BookingStatsMapper bookingStatsMapper;

    MyBatisBookingStatsAdapter(BookingStatsMapper bookingStatsMapper) {
        this.bookingStatsMapper = bookingStatsMapper;
    }

    @Override
    public List<SlotUtilization> findSlotUtilization(LocalDate from, LocalDate to) {
        return bookingStatsMapper.findSlotUtilization(toUtcStart(from), toUtcEnd(to));
    }

    private static LocalDateTime toUtcStart(LocalDate kstDate) {
        return kstDate.atStartOfDay(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static LocalDateTime toUtcEnd(LocalDate kstDate) {
        return kstDate.plusDays(1).atStartOfDay(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
