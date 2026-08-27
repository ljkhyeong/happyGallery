package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingTimeBlock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingTimeBlockPort {

    Optional<BookingTimeBlock> findById(Long id);

    List<BookingTimeBlock> findByDateBetweenOrderByDateAscStartTimeAsc(
            LocalDate dateFrom, LocalDate dateTo);

    <S extends BookingTimeBlock> S save(S block);

    boolean existsByDateAndStartTimeAndEndTime(
            LocalDate date, LocalTime startTime, LocalTime endTime);

    void delete(BookingTimeBlock block);
}
