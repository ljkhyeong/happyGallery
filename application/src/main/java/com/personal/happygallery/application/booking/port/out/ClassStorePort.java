package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;

public interface ClassStorePort {

    BookingClass save(BookingClass bookingClass);

    List<BookingClass> saveAll(List<BookingClass> classes);
}
