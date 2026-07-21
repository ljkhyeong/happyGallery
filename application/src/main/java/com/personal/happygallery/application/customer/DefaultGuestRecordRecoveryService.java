package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase;
import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.GuestRecordRecoveryTargetPort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.application.token.GuestTokenService.IssuedToken;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultGuestRecordRecoveryService implements GuestRecordRecoveryUseCase {

    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    private final GuestReaderPort guestReader;
    private final GuestRecordRecoveryTargetPort recoveryTargets;
    private final GuestPersonalDataProtector guestPersonalDataProtector;
    private final GuestTokenService guestTokenService;

    public DefaultGuestRecordRecoveryService(
            PhoneOwnershipVerificationUseCase phoneOwnershipVerification,
            GuestReaderPort guestReader,
            GuestRecordRecoveryTargetPort recoveryTargets,
            GuestPersonalDataProtector guestPersonalDataProtector,
            GuestTokenService guestTokenService) {
        this.phoneOwnershipVerification = phoneOwnershipVerification;
        this.guestReader = guestReader;
        this.recoveryTargets = recoveryTargets;
        this.guestPersonalDataProtector = guestPersonalDataProtector;
        this.guestTokenService = guestTokenService;
    }

    @Override
    public RecoveryResult recover(String phone, String verificationCode) {
        phoneOwnershipVerification.verify(phone, verificationCode);

        IssuedToken token = guestTokenService.issueRecoveryToken();
        return guestReader.findByPhoneHmac(guestPersonalDataProtector.indexPhone(phone))
                .map(guest -> recoverRecords(guest.getId(), token))
                .orElseGet(() -> new RecoveryResult(
                        token.rawToken(), token.expiresAt(), List.of(), List.of()));
    }

    private RecoveryResult recoverRecords(Long guestId, IssuedToken token) {
        List<Order> orders = recoveryTargets.findOrdersByGuestId(guestId);
        List<Booking> bookings = recoveryTargets.findBookingsByGuestId(guestId);

        orders.forEach(order -> order.replaceGuestAccessToken(token.tokenHash()));
        bookings.forEach(booking -> booking.replaceGuestAccessToken(token.tokenHash()));

        return new RecoveryResult(
                token.rawToken(),
                token.expiresAt(),
                orders.stream().map(RecoveredOrder::from).toList(),
                bookings.stream().map(RecoveredBooking::from).toList());
    }
}
