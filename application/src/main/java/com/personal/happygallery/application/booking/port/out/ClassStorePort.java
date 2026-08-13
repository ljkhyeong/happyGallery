package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingClass;
import java.util.List;

public interface ClassStorePort {

    <S extends BookingClass> S save(S bookingClass);

    <S extends BookingClass> List<S> saveAll(Iterable<S> classes);
}
