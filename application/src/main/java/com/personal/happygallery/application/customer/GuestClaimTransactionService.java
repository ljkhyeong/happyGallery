package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase.ClaimBookingSummary;
import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase.ClaimOrderSummary;
import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase.ClaimPreview;
import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase.ClaimResult;
import com.personal.happygallery.application.customer.port.out.GuestClaimTargetPort;
import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.monitoring.port.in.ClientMonitoringUseCase;
import com.personal.happygallery.application.notification.ReviewNotificationPublisher;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.error.DuplicateBookingException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.error.PhoneVerificationRequiredException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.user.User;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
class GuestClaimTransactionService {

    private static final Logger log = LoggerFactory.getLogger(GuestClaimTransactionService.class);
    private static final int PREVIEW_LIMIT = 100;

    private final UserReaderPort userReader;
    private final GuestReaderPort guestReader;
    private final PhoneVerificationConsumptionService phoneVerification;
    private final GuestClaimTargetPort claimTargets;
    private final ClientMonitoringUseCase clientMonitoringService;
    private final GuestPersonalDataProtector guestPersonalDataProtector;
    private final ReviewNotificationPublisher reviewNotificationPublisher;

    GuestClaimTransactionService(
            UserReaderPort userReader,
            GuestReaderPort guestReader,
            PhoneVerificationConsumptionService phoneVerification,
            GuestClaimTargetPort claimTargets,
            ClientMonitoringUseCase clientMonitoringService,
            GuestPersonalDataProtector guestPersonalDataProtector,
            ReviewNotificationPublisher reviewNotificationPublisher
    ) {
        this.userReader = userReader;
        this.guestReader = guestReader;
        this.phoneVerification = phoneVerification;
        this.claimTargets = claimTargets;
        this.clientMonitoringService = clientMonitoringService;
        this.guestPersonalDataProtector = guestPersonalDataProtector;
        this.reviewNotificationPublisher = reviewNotificationPublisher;
    }

    @Transactional(readOnly = true)
    public ClaimPreview preview(Long userId) {
        User user = findUser(userId);
        requirePhoneVerified(user);
        return buildPreview(user);
    }

    @Transactional
    public ClaimPreview verifyPhoneAndPreview(
            Long userId,
            String normalizedPhone,
            String verificationCode
    ) {
        User user = userReader.findByIdForUpdate(userId)
                .orElseThrow(NotFoundException.supplier("회원"));
        if (!Objects.equals(user.getPhone(), normalizedPhone)) {
            throw new PhoneVerificationFailedException();
        }
        phoneVerification.consume(
                normalizedPhone, verificationCode, PhoneVerificationPurpose.GUEST_CLAIM);
        user.markPhoneVerified();
        log.info("guest claim phone verified [userId={}]", userId);
        return buildPreview(user);
    }

    @Transactional
    public ClaimResult claim(
            Long userId,
            List<Long> orderIds,
            List<Long> bookingIds
    ) {
        User user = userReader.findByIdForUpdate(userId)
                .orElseThrow(NotFoundException.supplier("회원"));
        requirePhoneVerified(user);

        Guest guest = findGuest(user.getPhone()).orElse(null);
        if (guest == null) {
            return new ClaimResult(0, 0);
        }

        Set<Long> orderIdSet = new LinkedHashSet<>(orderIds);
        claimOrders(orderIdSet, guest.getId(), userId);

        Set<Long> bookingIdSet = new LinkedHashSet<>(bookingIds);
        claimBookings(bookingIdSet, guest.getId(), userId);

        clientMonitoringService.logGuestClaimCompleted(
                userId, guest.getId(), orderIdSet.size(), bookingIdSet.size());
        return new ClaimResult(orderIdSet.size(), bookingIdSet.size());
    }

    private void claimOrders(Set<Long> orderIds, Long guestId, Long userId) {
        if (orderIds.isEmpty()) {
            return;
        }
        Map<Long, Order> orderMap = claimTargets.findOrdersByIds(orderIds).stream()
                .collect(toMap(Order::getId, Function.identity()));
        for (Long orderId : orderIds) {
            Order order = orderMap.get(orderId);
            if (order == null || !Objects.equals(order.getGuestId(), guestId)) {
                throw new NotFoundException("claim 주문");
            }
            order.claimToUser(userId);
            if (order.getStatus().isReviewable()) {
                reviewNotificationPublisher.requestForOrder(userId, order.getId());
            }
        }
    }

    private void claimBookings(Set<Long> bookingIds, Long guestId, Long userId) {
        if (bookingIds.isEmpty()) {
            return;
        }
        Map<Long, Booking> bookingMap = claimTargets.findBookingsByIds(bookingIds).stream()
                .collect(toMap(Booking::getId, Function.identity()));
        for (Long bookingId : bookingIds) {
            Booking booking = bookingMap.get(bookingId);
            if (booking == null || booking.getGuest() == null
                    || !Objects.equals(booking.getGuest().getId(), guestId)) {
                throw new NotFoundException("claim 예약");
            }
        }

        Set<Long> bookedSlotIds = bookingMap.values().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.BOOKED)
                .map(booking -> booking.getSlot().getId())
                .collect(toSet());
        if (!bookedSlotIds.isEmpty()
                && claimTargets.existsBookedByUserIdAndSlotIds(userId, bookedSlotIds)) {
            throw new DuplicateBookingException();
        }

        for (Long bookingId : bookingIds) {
            Booking booking = bookingMap.get(bookingId);
            booking.claimToUser(userId);
            if (booking.getStatus().isReviewable()) {
                reviewNotificationPublisher.requestForBooking(userId, booking.getId());
            }
        }
    }

    private ClaimPreview buildPreview(User user) {
        return findGuest(user.getPhone())
                .map(guest -> new ClaimPreview(
                        user.isPhoneVerified(),
                        claimTargets.findOrdersByGuestId(guest.getId(), PREVIEW_LIMIT).stream()
                                .map(ClaimOrderSummary::from)
                                .toList(),
                        claimTargets.findBookingsByGuestId(guest.getId(), PREVIEW_LIMIT).stream()
                                .map(ClaimBookingSummary::from)
                                .toList()))
                .orElseGet(() -> new ClaimPreview(user.isPhoneVerified(), List.of(), List.of()));
    }

    private User findUser(Long userId) {
        return userReader.findById(userId)
                .orElseThrow(NotFoundException.supplier("회원"));
    }

    private Optional<Guest> findGuest(String phone) {
        return guestReader.findByPhoneHmac(guestPersonalDataProtector.indexPhone(phone));
    }

    private void requirePhoneVerified(User user) {
        if (!user.isPhoneVerified()) {
            throw new PhoneVerificationRequiredException();
        }
    }
}
