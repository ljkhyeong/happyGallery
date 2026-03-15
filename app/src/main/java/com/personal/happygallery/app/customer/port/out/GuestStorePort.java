package com.personal.happygallery.app.customer.port.out;

import com.personal.happygallery.domain.booking.Guest;

public interface GuestStorePort {
    Guest save(Guest guest);
}
