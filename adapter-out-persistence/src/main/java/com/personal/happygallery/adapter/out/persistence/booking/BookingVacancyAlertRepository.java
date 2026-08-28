package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingVacancyAlertPort;
import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface BookingVacancyAlertRepository
        extends JpaRepository<BookingVacancyAlert, Long>, BookingVacancyAlertPort {

    @Override
    <S extends BookingVacancyAlert> S save(S alert);

    @Override
    <S extends BookingVacancyAlert> List<S> saveAll(Iterable<S> alerts);

    @Override
    @Query("""
            SELECT a FROM BookingVacancyAlert a
            WHERE a.slot.id = :slotId AND a.guestId = :guestId
              AND a.status = com.personal.happygallery.domain.booking.VacancyAlertStatus.WAITING
            """)
    Optional<BookingVacancyAlert> findWaitingBySlotIdAndGuestId(
            @Param("slotId") Long slotId, @Param("guestId") Long guestId);

    @Override
    @Query("""
            SELECT a FROM BookingVacancyAlert a
            WHERE a.slot.id = :slotId AND a.userId = :userId
              AND a.status = com.personal.happygallery.domain.booking.VacancyAlertStatus.WAITING
            """)
    Optional<BookingVacancyAlert> findWaitingBySlotIdAndUserId(
            @Param("slotId") Long slotId, @Param("userId") Long userId);

    @Override
    @Query("""
            SELECT a FROM BookingVacancyAlert a
            JOIN FETCH a.slot
            WHERE a.userId = :userId
              AND a.status = com.personal.happygallery.domain.booking.VacancyAlertStatus.WAITING
            ORDER BY a.id
            """)
    List<BookingVacancyAlert> findWaitingByUserId(@Param("userId") Long userId);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM BookingVacancyAlert a
            WHERE a.slot.id = :slotId AND a.accessTokenHash = :accessTokenHash
              AND a.status = com.personal.happygallery.domain.booking.VacancyAlertStatus.WAITING
            """)
    Optional<BookingVacancyAlert> findWaitingBySlotIdAndAccessTokenHashForUpdate(
            @Param("slotId") Long slotId, @Param("accessTokenHash") String accessTokenHash);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM BookingVacancyAlert a
            WHERE a.slot.id = :slotId AND a.userId = :userId
              AND a.status = com.personal.happygallery.domain.booking.VacancyAlertStatus.WAITING
            """)
    Optional<BookingVacancyAlert> findWaitingBySlotIdAndUserIdForUpdate(
            @Param("slotId") Long slotId, @Param("userId") Long userId);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM BookingVacancyAlert a
            WHERE a.slot.id = :slotId
              AND a.status = com.personal.happygallery.domain.booking.VacancyAlertStatus.WAITING
            ORDER BY a.id
            """)
    List<BookingVacancyAlert> findWaitingBySlotIdForUpdate(@Param("slotId") Long slotId);
}
