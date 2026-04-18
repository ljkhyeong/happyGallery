package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.Guest;

public interface GuestStorePort {
    Guest save(Guest guest);
}
