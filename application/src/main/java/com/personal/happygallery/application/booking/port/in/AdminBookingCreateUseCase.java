package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.BookingSource;

public interface AdminBookingCreateUseCase {

    AdminBookingResponse create(CreateAdminBookingCommand command);

    record CreateAdminBookingCommand(
            Long slotId,
            String name,
            String phone,
            int participantCount,
            BookingSource source,
            boolean depositPaid,
            Long adminId
    ) {
    }
}
