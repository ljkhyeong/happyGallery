package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.GuestBookingSummary;
import com.personal.happygallery.application.customer.port.in.GuestOrderSummary;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase;
import com.personal.happygallery.application.customer.port.out.GuestRecordRecoveryTargetPort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.CursorUtils.CursorParam;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultGuestRecordRecoveryService implements GuestRecordRecoveryUseCase {

    private final PhoneVerificationAttemptGuard attemptGuard;
    private final GuestRecordRecoveryTransactionService transactionService;
    private final GuestRecordRecoveryTargetPort recoveryTargets;
    private final GuestTokenService guestTokenService;

    public DefaultGuestRecordRecoveryService(
            PhoneVerificationAttemptGuard attemptGuard,
            GuestRecordRecoveryTransactionService transactionService,
            GuestRecordRecoveryTargetPort recoveryTargets,
            GuestTokenService guestTokenService) {
        this.attemptGuard = attemptGuard;
        this.transactionService = transactionService;
        this.recoveryTargets = recoveryTargets;
        this.guestTokenService = guestTokenService;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public RecoveryResult recover(String phone, String verificationCode) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        attemptGuard.check(normalizedPhone);
        return transactionService.recover(normalizedPhone, verificationCode);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<GuestOrderSummary> listRecoveredOrders(
            String accessToken, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        String tokenHash = guestTokenService.resolveTokenHash(accessToken);
        CursorParam after = CursorUtils.decodeOrFirst(cursor);
        List<Order> orders = recoveryTargets.findOrdersByAccessToken(
                tokenHash, after.timestamp(), after.id(), pageSize + 1);
        CursorPage<Order> page = CursorPage.of(
                orders,
                pageSize,
                order -> CursorUtils.encode(order.getCreatedAt(), order.getId()));
        return new CursorPage<>(
                page.content().stream().map(GuestOrderSummary::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<GuestBookingSummary> listRecoveredBookings(
            String accessToken, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        String tokenHash = guestTokenService.resolveTokenHash(accessToken);
        CursorParam after = CursorUtils.decodeOrFirst(cursor);
        List<Booking> bookings = recoveryTargets.findBookingsByAccessToken(
                tokenHash, after.timestamp(), after.id(), pageSize + 1);
        CursorPage<Booking> page = CursorPage.of(
                bookings,
                pageSize,
                booking -> CursorUtils.encode(booking.getCreatedAt(), booking.getId()));
        return new CursorPage<>(
                page.content().stream().map(GuestBookingSummary::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
