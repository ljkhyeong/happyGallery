package com.personal.happygallery.app.booking.port.out;

import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;
import java.util.Optional;

public interface ClassReaderPort {

    Optional<BookingClass> findById(Long id);

    List<BookingClass> findAll();

    long count();
}
