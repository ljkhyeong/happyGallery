package com.personal.happygallery.app.customer;

import com.personal.happygallery.app.customer.port.out.GuestReaderPort;
import com.personal.happygallery.app.customer.port.out.GuestStorePort;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.infra.booking.GuestRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link GuestRepository}(infra) → {@link GuestReaderPort} + {@link GuestStorePort}(app) 브릿지 어댑터.
 */
@Component
class GuestReaderPortAdapter implements GuestReaderPort, GuestStorePort {

    private final GuestRepository guestRepository;

    GuestReaderPortAdapter(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    @Override
    public Optional<Guest> findById(Long id) {
        return guestRepository.findById(id);
    }

    @Override
    public Optional<Guest> findByPhone(String phone) {
        return guestRepository.findByPhone(phone);
    }

    @Override
    public Guest save(Guest guest) {
        return guestRepository.save(guest);
    }
}
