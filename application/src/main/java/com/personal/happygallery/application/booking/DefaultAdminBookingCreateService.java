package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.AdminBookingCreateUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingResponse;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryActor;
import com.personal.happygallery.domain.booking.DepositCalculator;
import com.personal.happygallery.domain.booking.DepositCalculator.BookingAmounts;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultAdminBookingCreateService implements AdminBookingCreateUseCase {

    private final GuestStorePort guestStore;
    private final GuestPersonalDataProtector guestProtector;
    private final BookingCreationSupport creationSupport;
    private final Clock clock;

    public DefaultAdminBookingCreateService(
            GuestStorePort guestStore,
            GuestPersonalDataProtector guestProtector,
            BookingCreationSupport creationSupport,
            Clock clock
    ) {
        this.guestStore = guestStore;
        this.guestProtector = guestProtector;
        this.creationSupport = creationSupport;
        this.clock = clock;
    }

    @Override
    public AdminBookingResponse create(CreateAdminBookingCommand command) {
        command.source().requireOperatorManaged();
        Guest guest = guestStore.getOrCreateByPhoneHmac(
                guestProtector.newGuest(command.name(), command.phone()));
        Slot slot = creationSupport.reserveSlot(
                guest.getPhoneHmac(), command.slotId(), command.participantCount());
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
        booking = creationSupport.saveAndComplete(
                booking, slot, BookingHistoryActor.ADMIN, command.adminId(),
                "접수 경로: " + command.source().name());

        return AdminBookingResponse.fromGuest(
                booking,
                guestProtector.decryptName(guest),
                guestProtector.decryptPhone(guest));
    }
}
