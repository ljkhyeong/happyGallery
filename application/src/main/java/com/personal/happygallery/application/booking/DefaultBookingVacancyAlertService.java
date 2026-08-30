package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingVacancyAlertUseCase;
import com.personal.happygallery.application.booking.port.out.BookingVacancyAlertPort;
import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PhoneVerificationRequiredException;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultBookingVacancyAlertService implements BookingVacancyAlertUseCase {

    private final BookingVacancyAlertPort alertPort;
    private final VerifiedGuestResolver verifiedGuestResolver;
    private final UserReaderPort userReaderPort;
    private final GuestTokenService guestTokenService;
    private final SlotCapacitySupport slotCapacitySupport;
    private final Clock clock;

    public DefaultBookingVacancyAlertService(
            BookingVacancyAlertPort alertPort,
            VerifiedGuestResolver verifiedGuestResolver,
            UserReaderPort userReaderPort,
            GuestTokenService guestTokenService,
            SlotCapacitySupport slotCapacitySupport,
            Clock clock
    ) {
        this.alertPort = alertPort;
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.userReaderPort = userReaderPort;
        this.guestTokenService = guestTokenService;
        this.slotCapacitySupport = slotCapacitySupport;
        this.clock = clock;
    }

    @Override
    public GuestAlertResult registerGuest(GuestAlertCommand command) {
        Slot slot = requireFullSlot(command.slotId());
        Guest guest = verifiedGuestResolver.resolveWithVerificationCode(
                command.phone(), command.verificationCode(), command.name());
        GuestTokenService.IssuedToken token = guestTokenService.issue();
        BookingVacancyAlert alert = alertPort
                .findWaitingBySlotIdAndGuestId(slot.getId(), guest.getId())
                .map(existing -> {
                    existing.rotateAccessToken(token.tokenHash());
                    return existing;
                })
                .orElseGet(() -> BookingVacancyAlert.forGuest(
                        slot, guest.getId(), token.tokenHash()));
        return new GuestAlertResult(alertPort.save(alert), token.rawToken());
    }

    @Override
    public BookingVacancyAlert registerMember(Long slotId, Long userId) {
        User user = userReaderPort.findById(userId)
                .filter(User::isActive)
                .orElseThrow(NotFoundException.supplier("회원"));
        if (!user.isPhoneVerified()) {
            throw new PhoneVerificationRequiredException();
        }
        Slot slot = requireFullSlot(slotId);
        return alertPort.findWaitingBySlotIdAndUserId(slotId, userId)
                .orElseGet(() -> alertPort.save(BookingVacancyAlert.forUser(slot, userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingVacancyAlert> listMember(Long userId) {
        return alertPort.findWaitingByUserId(userId);
    }

    @Override
    public void cancelGuest(Long slotId, String accessToken) {
        String tokenHash = guestTokenService.resolveTokenHash(accessToken);
        BookingVacancyAlert alert = alertPort
                .findWaitingBySlotIdAndAccessTokenHashForUpdate(slotId, tokenHash)
                .orElseThrow(NotFoundException.supplier("빈자리 알림"));
        alert.cancel(LocalDateTime.now(clock));
        alertPort.save(alert);
    }

    @Override
    public void cancelMember(Long slotId, Long userId) {
        alertPort.findWaitingBySlotIdAndUserIdForUpdate(slotId, userId)
                .ifPresent(alert -> {
                    alert.cancel(LocalDateTime.now(clock));
                    alertPort.save(alert);
                });
    }

    private Slot requireFullSlot(Long slotId) {
        Slot slot = slotCapacitySupport.lockCapacityScope(slotId).source();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!slot.isReservableAt(now) || slot.getBookedCount() < slot.getCapacity()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "현재 만석인 예약 가능 회차에만 빈자리 알림을 신청할 수 있습니다.");
        }
        return slot;
    }
}
