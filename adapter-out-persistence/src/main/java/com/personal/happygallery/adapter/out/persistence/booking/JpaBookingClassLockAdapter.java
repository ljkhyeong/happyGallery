package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingClassLockPort;
import com.personal.happygallery.domain.booking.BookingClass;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaBookingClassLockAdapter implements BookingClassLockPort {

    private final EntityManager entityManager;

    JpaBookingClassLockAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<BookingClass> lockFresh(Long classId) {
        entityManager.flush();
        BookingClass bookingClass = entityManager.find(
                BookingClass.class, classId, LockModeType.PESSIMISTIC_WRITE);
        if (bookingClass == null) {
            return Optional.empty();
        }
        entityManager.refresh(bookingClass, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(bookingClass);
    }
}
