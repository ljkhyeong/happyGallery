package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.booking.Booking;

public record BookingNoShowResponse(
        Long bookingId,
        String status
) {

    public static BookingNoShowResponse from(Booking booking) {
        return new BookingNoShowResponse(booking.getId(), booking.getStatus().name());
    }
}
