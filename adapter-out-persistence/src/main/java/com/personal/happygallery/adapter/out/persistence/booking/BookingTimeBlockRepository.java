package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingTimeBlockPort;
import com.personal.happygallery.domain.booking.BookingTimeBlock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingTimeBlockRepository
        extends JpaRepository<BookingTimeBlock, Long>, BookingTimeBlockPort {

    @Override
    Optional<BookingTimeBlock> findById(Long id);

    @Override
    List<BookingTimeBlock> findByDateBetweenOrderByDateAscStartTimeAsc(
            LocalDate dateFrom, LocalDate dateTo);

    @Override
    <S extends BookingTimeBlock> S save(S block);

    @Override
    boolean existsByDateAndStartTimeAndEndTime(
            LocalDate date, LocalTime startTime, LocalTime endTime);

    @Override
    void delete(BookingTimeBlock block);
}
