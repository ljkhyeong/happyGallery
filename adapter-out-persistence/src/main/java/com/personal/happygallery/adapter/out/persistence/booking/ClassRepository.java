package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassRepository extends JpaRepository<BookingClass, Long>, ClassReaderPort, ClassStorePort {

    @Override
    <S extends BookingClass> S save(S bookingClass);

    @Override
    <S extends BookingClass> List<S> saveAll(Iterable<S> classes);

    @Override Optional<BookingClass> findById(Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BookingClass c WHERE c.id = :id")
    Optional<BookingClass> findByIdForUpdate(@Param("id") Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BookingClass c WHERE c.id IN :ids ORDER BY c.id")
    List<BookingClass> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    List<BookingClass> findAllByOrderByCreatedAtDescIdDesc();

    @Override
    default List<BookingClass> findAll() {
        return findAllByOrderByCreatedAtDescIdDesc();
    }

    List<BookingClass> findByStatusOrderByCreatedAtDescIdDesc(BookingClassStatus status);

    @Override
    default List<BookingClass> findAllActive() {
        return findByStatusOrderByCreatedAtDescIdDesc(BookingClassStatus.ACTIVE);
    }

}
