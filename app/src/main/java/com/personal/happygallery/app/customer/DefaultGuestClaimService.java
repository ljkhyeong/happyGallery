package com.personal.happygallery.app.customer;

import com.personal.happygallery.app.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.app.customer.port.out.GuestClaimQueryPort;
import com.personal.happygallery.app.customer.port.out.GuestReaderPort;
import com.personal.happygallery.app.customer.port.out.PhoneVerificationPort;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.monitoring.port.in.ClientMonitoringUseCase;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.error.PhoneVerificationFailedException;
import com.personal.happygallery.common.error.PhoneVerificationRequiredException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultGuestClaimService implements GuestClaimUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultGuestClaimService.class);

    private final UserReaderPort userReader;
    private final GuestReaderPort guestReader;
    private final PhoneVerificationPort phoneVerificationPort;
    private final GuestClaimQueryPort claimQuery;
    private final Clock clock;
    private final ClientMonitoringUseCase clientMonitoringUseCase;

    public DefaultGuestClaimService(UserReaderPort userReader,
                             GuestReaderPort guestReader,
                             PhoneVerificationPort phoneVerificationPort,
                             GuestClaimQueryPort claimQuery,
                             Clock clock,
                             ClientMonitoringUseCase clientMonitoringUseCase) {
        this.userReader = userReader;
        this.guestReader = guestReader;
        this.phoneVerificationPort = phoneVerificationPort;
        this.claimQuery = claimQuery;
        this.clock = clock;
        this.clientMonitoringUseCase = clientMonitoringUseCase;
    }

    @Transactional(readOnly = true)
    public ClaimPreview preview(Long userId) {
        User user = findUser(userId);
        requirePhoneVerified(user);
        return buildPreview(user);
    }

    public ClaimPreview verifyPhoneAndPreview(Long userId, String verificationCode) {
        User user = findUser(userId);
        PhoneVerification verification = findValidVerification(user.getPhone(), verificationCode);
        verification.markVerified();
        user.markPhoneVerified();
        log.info("guest claim phone verified [userId={} phone={}]", userId, maskPhone(user.getPhone()));
        return buildPreview(user);
    }

    public ClaimResult claim(Long userId,
                             List<Long> orderIds,
                             List<Long> bookingIds,
                             List<Long> passIds) {
        User user = findUser(userId);
        requirePhoneVerified(user);

        Guest guest = findGuestByAnyPhoneFormat(user.getPhone())
                .orElse(null);
        if (guest == null) {
            return new ClaimResult(0, 0, 0);
        }

        Set<Long> orderIdSet = dedupe(orderIds);
        claimOrders(orderIdSet, guest.getId(), userId);

        Set<Long> bookingIdSet = dedupe(bookingIds);
        Set<Long> passIdsToClaim = dedupe(passIds);
        claimBookings(bookingIdSet, guest.getId(), userId, passIdsToClaim);

        int claimedPassCount = claimPasses(passIdsToClaim, guest.getId(), userId);

        clientMonitoringUseCase.logGuestClaimCompleted(
                userId, guest.getId(),
                orderIdSet.size(), bookingIdSet.size(), claimedPassCount);
        return new ClaimResult(orderIdSet.size(), bookingIdSet.size(), claimedPassCount);
    }

    private void claimOrders(Set<Long> orderIds, Long guestId, Long userId) {
        if (orderIds.isEmpty()) return;
        Map<Long, Order> orderMap = claimQuery.findOrdersByIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        for (Long orderId : orderIds) {
            Order order = orderMap.get(orderId);
            if (order == null || !Objects.equals(order.getGuestId(), guestId) || order.getUserId() != null) {
                throw new NotFoundException("claim 주문");
            }
            order.claimToUser(userId);
        }
    }

    private void claimBookings(Set<Long> bookingIds, Long guestId, Long userId,
                               Set<Long> passIdsToClaim) {
        if (bookingIds.isEmpty()) return;
        Map<Long, Booking> bookingMap = claimQuery.findBookingsByIds(bookingIds).stream()
                .collect(Collectors.toMap(Booking::getId, Function.identity()));
        for (Long bookingId : bookingIds) {
            Booking booking = bookingMap.get(bookingId);
            if (booking == null || booking.getGuest() == null
                    || !Objects.equals(booking.getGuest().getId(), guestId)
                    || booking.getUserId() != null) {
                throw new NotFoundException("claim 예약");
            }
            booking.claimToUser(userId);

            if (booking.getPassPurchase() != null
                    && booking.getPassPurchase().getGuest() != null
                    && Objects.equals(booking.getPassPurchase().getGuest().getId(), guestId)
                    && booking.getPassPurchase().getUserId() == null) {
                passIdsToClaim.add(booking.getPassPurchase().getId());
            }
        }
    }

    private int claimPasses(Set<Long> passIds, Long guestId, Long userId) {
        if (passIds.isEmpty()) return 0;
        Map<Long, PassPurchase> passMap = claimQuery.findPassPurchasesByIds(passIds).stream()
                .collect(Collectors.toMap(PassPurchase::getId, Function.identity()));
        int count = 0;
        for (Long passId : passIds) {
            PassPurchase pass = passMap.get(passId);
            if (pass == null || pass.getGuest() == null
                    || !Objects.equals(pass.getGuest().getId(), guestId)
                    || pass.getUserId() != null) {
                throw new NotFoundException("claim 8회권");
            }
            pass.claimToUser(userId);
            count++;
        }
        return count;
    }

    private static final int PREVIEW_LIMIT = 100;

    private ClaimPreview buildPreview(User user) {
        return findGuestByAnyPhoneFormat(user.getPhone())
                .map(guest -> new ClaimPreview(
                        user.isPhoneVerified(),
                        claimQuery.findOrdersByGuestId(guest.getId()).stream()
                                .limit(PREVIEW_LIMIT)
                                .map(ClaimOrderSummary::from)
                                .toList(),
                        claimQuery.findBookingsByGuestId(guest.getId()).stream()
                                .limit(PREVIEW_LIMIT)
                                .map(ClaimBookingSummary::from)
                                .toList(),
                        claimQuery.findPassPurchasesByGuestId(guest.getId()).stream()
                                .limit(PREVIEW_LIMIT)
                                .map(ClaimPassSummary::from)
                                .toList()))
                .orElseGet(() -> new ClaimPreview(user.isPhoneVerified(), List.of(), List.of(), List.of()));
    }

    private User findUser(Long userId) {
        return userReader.findById(userId)
                .orElseThrow(() -> new NotFoundException("회원"));
    }

    private PhoneVerification findValidVerification(String phone, String verificationCode) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (String candidatePhone : candidatePhones(phone)) {
            Optional<PhoneVerification> verification = phoneVerificationPort
                    .findValidVerification(candidatePhone, verificationCode, now);
            if (verification.isPresent()) {
                return verification.get();
            }
        }
        throw new PhoneVerificationFailedException();
    }

    private Optional<Guest> findGuestByAnyPhoneFormat(String phone) {
        for (String candidatePhone : candidatePhones(phone)) {
            Optional<Guest> guest = guestReader.findByPhone(candidatePhone);
            if (guest.isPresent()) {
                return guest;
            }
        }
        return Optional.empty();
    }

    private void requirePhoneVerified(User user) {
        if (!user.isPhoneVerified()) {
            throw new PhoneVerificationRequiredException();
        }
    }

    private static List<String> candidatePhones(String phone) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(phone);

        String digitsOnly = phone.replaceAll("\\D", "");
        if (!digitsOnly.isBlank()) {
            candidates.add(digitsOnly);
        }
        return List.copyOf(candidates);
    }

    private static Set<Long> dedupe(List<Long> ids) {
        return ids == null ? Set.of() : new LinkedHashSet<>(ids);
    }

    /** 전화번호 뒤 4자리를 마스킹한다. */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
