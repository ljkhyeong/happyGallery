package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaBookingPersistenceAdapter implements BookingStorePort, ClassStorePort, SlotStorePort {

    private final BookingRepository bookingRepository;
    private final ClassRepository classRepository;
    private final SlotRepository slotRepository;

    JpaBookingPersistenceAdapter(
            BookingRepository bookingRepository,
            ClassRepository classRepository,
            SlotRepository slotRepository) {
        this.bookingRepository = bookingRepository;
        this.classRepository = classRepository;
        this.slotRepository = slotRepository;
    }

    @Override
    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    @Override
    public int updateBookedOwnerPhoneHmacByUserId(Long userId, String ownerPhoneHmac) {
        return bookingRepository.updateBookedOwnerPhoneHmacByUserId(userId, ownerPhoneHmac);
    }

    @Override
    public BookingClass save(BookingClass bookingClass) {
        return classRepository.save(bookingClass);
    }

    @Override
    public List<BookingClass> saveAll(List<BookingClass> classes) {
        return classRepository.saveAll(classes);
    }

    @Override
    public Slot save(Slot slot) {
        return slotRepository.save(slot);
    }
}
