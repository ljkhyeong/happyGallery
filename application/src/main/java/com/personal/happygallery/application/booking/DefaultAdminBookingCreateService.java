package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.AdminBookingCreateUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingResponse;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.DepositCalculator;
import com.personal.happygallery.domain.booking.DepositCalculator.BookingAmounts;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.DuplicateBookingException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultAdminBookingCreateService implements AdminBookingCreateUseCase {

    private final BookingReaderPort bookingReader;
    private final BookingStorePort bookingStore;
    private final GuestStorePort guestStore;
    private final GuestPersonalDataProtector guestProtector;
    private final SlotCapacitySupport slotCapacity;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public DefaultAdminBookingCreateService(
            BookingReaderPort bookingReader,
            BookingStorePort bookingStore,
            GuestStorePort guestStore,
            GuestPersonalDataProtector guestProtector,
            SlotCapacitySupport slotCapacity,
            BookingSupport bookingSupport,
            Clock clock
    ) {
        this.bookingReader = bookingReader;
        this.bookingStore = bookingStore;
        this.guestStore = guestStore;
        this.guestProtector = guestProtector;
        this.slotCapacity = slotCapacity;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    @Override
    public AdminBookingResponse create(CreateAdminBookingCommand command) {
        command.source().requireOperatorManaged();
        slotCapacity.requireAvailableSlot(command.slotId());

        Guest guest = guestStore.getOrCreateByPhoneHmac(
                guestProtector.newGuest(command.name(), command.phone()));
        if (bookingReader.existsBookedBySlotIdAndGuestId(command.slotId(), guest.getId())) {
            throw new DuplicateBookingException();
        }

        Slot slot = slotCapacity.reserveCapacity(
                command.slotId(), command.participantCount());
        BookingAmounts standardAmounts =
                DepositCalculator.calculate(slot, command.participantCount());
        long depositAmount = command.depositPaid()
                ? standardAmounts.depositAmount()
                : 0L;
        long balanceAmount = command.depositPaid()
                ? standardAmounts.balanceAmount()
                : Math.addExact(
                        standardAmounts.depositAmount(),
                        standardAmounts.balanceAmount());

        Booking booking = Booking.forAdminGuest(
                guest,
                slot,
                command.participantCount(),
                depositAmount,
                balanceAmount,
                command.source(),
                command.depositPaid() ? LocalDateTime.now(clock) : null);
        booking = bookingStore.save(booking);

        bookingSupport.recordHistory(
                booking,
                BookingHistoryAction.BOOKED,
                null,
                slot,
                "ADMIN",
                command.adminId(),
                "접수 경로: " + command.source().name());
        bookingSupport.notifyBooker(booking, NotificationEventType.BOOKING_CONFIRMED);

        return AdminBookingResponse.fromGuest(
                booking,
                guestProtector.decryptName(guest),
                guestProtector.decryptPhone(guest));
    }
}
