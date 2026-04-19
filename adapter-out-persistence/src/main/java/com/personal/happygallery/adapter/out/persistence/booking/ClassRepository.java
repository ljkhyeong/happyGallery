package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<BookingClass, Long>, ClassReaderPort, ClassStorePort {

    @Override Optional<BookingClass> findById(Long id);
    @Override BookingClass save(BookingClass bookingClass);

    @Override
    default List<BookingClass> saveAll(List<BookingClass> classes) {
        return saveAll((Iterable<BookingClass>) classes);
    }
}
