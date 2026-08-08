package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase;
import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.GuestRecordRecoveryTargetPort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.application.token.GuestTokenService.IssuedToken;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.order.Order;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultGuestRecordRecoveryService implements GuestRecordRecoveryUseCase {

    private static final int LEGACY_RECOVERY_SUMMARY_LIMIT = PageParams.MAX_SIZE;

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
    @Transactional
    public RecoveryResult recover(String phone, String verificationCode) {
        phoneOwnershipVerification.verify(
                phone, verificationCode, PhoneVerificationPurpose.GUEST_RECORD_RECOVERY);

        IssuedToken token = guestTokenService.issueRecoveryToken();
        return guestReader.findByPhoneHmac(guestPersonalDataProtector.indexPhone(phone))
                .map(guest -> recoverRecords(guest.getId(), token))
                .orElseGet(() -> new RecoveryResult(
                        token.rawToken(), token.expiresAt(), List.of(), List.of()));
    }

    private RecoveryResult recoverRecords(Long guestId, IssuedToken token) {
        recoveryTargets.replaceOrderAccessTokens(guestId, token.tokenHash());
        recoveryTargets.replaceBookingAccessTokens(guestId, token.tokenHash());

        List<Order> orders = recoveryTargets.findOrdersByGuestId(
                guestId, LEGACY_RECOVERY_SUMMARY_LIMIT);
        List<Booking> bookings = recoveryTargets.findBookingsByGuestId(
                guestId, LEGACY_RECOVERY_SUMMARY_LIMIT);

        return new RecoveryResult(
                token.rawToken(),
                token.expiresAt(),
                orders.stream().map(RecoveredOrder::from).toList(),
                bookings.stream().map(RecoveredBooking::from).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<RecoveredOrder> listRecoveredOrders(
            String accessToken, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        int fetchSize = pageSize + 1;
        String tokenHash = guestTokenService.resolveTokenHash(accessToken);
        List<Order> orders;
        if (cursor == null) {
            orders = recoveryTargets.findOrdersByAccessToken(tokenHash, fetchSize);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            orders = recoveryTargets.findOrdersByAccessTokenAfter(
                    tokenHash, cursorParam.timestamp(), cursorParam.id(), fetchSize);
        }
        CursorPage<Order> page = CursorPage.of(
                orders,
                pageSize,
                order -> CursorUtils.encode(order.getCreatedAt(), order.getId()));
        return new CursorPage<>(
                page.content().stream().map(RecoveredOrder::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<RecoveredBooking> listRecoveredBookings(
            String accessToken, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        int fetchSize = pageSize + 1;
        String tokenHash = guestTokenService.resolveTokenHash(accessToken);
        List<Booking> bookings;
        if (cursor == null) {
            bookings = recoveryTargets.findBookingsByAccessToken(tokenHash, fetchSize);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            bookings = recoveryTargets.findBookingsByAccessTokenAfter(
                    tokenHash, cursorParam.timestamp(), cursorParam.id(), fetchSize);
        }
        CursorPage<Booking> page = CursorPage.of(
                bookings,
                pageSize,
                booking -> CursorUtils.encode(booking.getCreatedAt(), booking.getId()));
        return new CursorPage<>(
                page.content().stream().map(RecoveredBooking::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
