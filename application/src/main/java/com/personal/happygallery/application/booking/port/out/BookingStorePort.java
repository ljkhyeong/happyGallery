package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Booking;

public interface BookingStorePort {

    <S extends Booking> S save(S booking);

    int updateBookedOwnerPhoneHmacByUserId(Long userId, String ownerPhoneHmac);
}
