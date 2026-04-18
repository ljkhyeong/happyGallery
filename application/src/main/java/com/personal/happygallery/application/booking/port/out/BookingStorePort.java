package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Booking;

public interface BookingStorePort {
    Booking save(Booking booking);
}
