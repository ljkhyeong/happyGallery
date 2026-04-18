package com.personal.happygallery.adapter.out.persistence.dashboard.adapter;

import com.personal.happygallery.application.dashboard.dto.SlotUtilization;
import com.personal.happygallery.application.dashboard.port.out.BookingStatsQueryPort;
import com.personal.happygallery.adapter.out.persistence.time.SeoulDateTimeRangeConverter;
import com.personal.happygallery.adapter.out.persistence.dashboard.mapper.BookingStatsMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class MyBatisBookingStatsAdapter implements BookingStatsQueryPort {

    private final BookingStatsMapper bookingStatsMapper;

    MyBatisBookingStatsAdapter(BookingStatsMapper bookingStatsMapper) {
        this.bookingStatsMapper = bookingStatsMapper;
    }

    @Override
    public List<SlotUtilization> findSlotUtilization(LocalDate from, LocalDate to) {
        return bookingStatsMapper.findSlotUtilization(
                SeoulDateTimeRangeConverter.toUtcStart(from),
                SeoulDateTimeRangeConverter.toUtcExclusiveEnd(to));
    }
}
