package com.personal.happygallery.app.booking;

import com.personal.happygallery.domain.booking.Slot;

interface SlotBookingCoordinator {

    void confirmBooking(Long slotId);

    Slot releaseSlotCapacity(Long slotId);
}
