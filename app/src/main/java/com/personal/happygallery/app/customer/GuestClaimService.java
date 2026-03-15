package com.personal.happygallery.app.customer;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.error.PhoneVerificationFailedException;
import com.personal.happygallery.common.error.PhoneVerificationRequiredException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.infra.user.UserRepository;
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
public class GuestClaimService {

    private static final Logger log = LoggerFactory.getLogger(GuestClaimService.class);

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final PassPurchaseRepository passPurchaseRepository;
    private final Clock clock;

    public GuestClaimService(UserRepository userRepository,
                             GuestRepository guestRepository,
                             PhoneVerificationRepository phoneVerificationRepository,
                             OrderRepository orderRepository,
                             BookingRepository bookingRepository,
                             PassPurchaseRepository passPurchaseRepository,
                             Clock clock) {
        this.userRepository = userRepository;
        this.guestRepository = guestRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
        this.passPurchaseRepository = passPurchaseRepository;
        this.clock = clock;
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
        log.info("guest claim phone verified [userId={} phone={}]", userId, user.getPhone());
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

        // 주문 일괄 조회 → 소유권 검증 → claim
        Set<Long> orderIdSet = dedupe(orderIds);
        Map<Long, Order> orderMap = orderRepository.findAllById(orderIdSet).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        for (Long orderId : orderIdSet) {
            Order order = orderMap.get(orderId);
            if (order == null || !Objects.equals(order.getGuestId(), guest.getId()) || order.getUserId() != null) {
                throw new NotFoundException("claim 주문");
            }
            order.claimToUser(userId);
        }

        // 예약 일괄 조회 → 소유권 검증 → claim + 연결된 pass 수집
        Set<Long> bookingIdSet = dedupe(bookingIds);
        Map<Long, Booking> bookingMap = bookingRepository.findAllById(bookingIdSet).stream()
                .collect(Collectors.toMap(Booking::getId, Function.identity()));
        Set<Long> passIdsToClaim = dedupe(passIds);
        for (Long bookingId : bookingIdSet) {
            Booking booking = bookingMap.get(bookingId);
            if (booking == null || booking.getGuest() == null
                    || !Objects.equals(booking.getGuest().getId(), guest.getId())
                    || booking.getUserId() != null) {
                throw new NotFoundException("claim 예약");
            }
            booking.claimToUser(userId);

            if (booking.getPassPurchase() != null
                    && booking.getPassPurchase().getGuest() != null
                    && Objects.equals(booking.getPassPurchase().getGuest().getId(), guest.getId())
                    && booking.getPassPurchase().getUserId() == null) {
                passIdsToClaim.add(booking.getPassPurchase().getId());
            }
        }

        // 8회권 일괄 조회 → 소유권 검증 → claim
        Map<Long, PassPurchase> passMap = passPurchaseRepository.findAllById(passIdsToClaim).stream()
                .collect(Collectors.toMap(PassPurchase::getId, Function.identity()));
        int claimedPassCount = 0;
        for (Long passId : passIdsToClaim) {
            PassPurchase pass = passMap.get(passId);
            if (pass == null || pass.getGuest() == null
                    || !Objects.equals(pass.getGuest().getId(), guest.getId())
                    || pass.getUserId() != null) {
                throw new NotFoundException("claim 8회권");
            }
            pass.claimToUser(userId);
            claimedPassCount++;
        }

        log.info("guest claim completed [userId={} guestId={} orders={} bookings={} passes={}]",
                userId, guest.getId(), orderIdSet.size(), bookingIdSet.size(), claimedPassCount);
        return new ClaimResult(orderIdSet.size(), bookingIdSet.size(), claimedPassCount);
    }

    private static final int PREVIEW_LIMIT = 100;

    private ClaimPreview buildPreview(User user) {
        return findGuestByAnyPhoneFormat(user.getPhone())
                .map(guest -> new ClaimPreview(
                        user.isPhoneVerified(),
                        orderRepository.findByGuestIdOrderByCreatedAtDesc(guest.getId()).stream()
                                .limit(PREVIEW_LIMIT)
                                .map(ClaimOrderSummary::from)
                                .toList(),
                        bookingRepository.findByGuestIdWithDetails(guest.getId()).stream()
                                .limit(PREVIEW_LIMIT)
                                .map(ClaimBookingSummary::from)
                                .toList(),
                        passPurchaseRepository.findByGuestIdOrderByPurchasedAtDesc(guest.getId()).stream()
                                .limit(PREVIEW_LIMIT)
                                .map(ClaimPassSummary::from)
                                .toList()))
                .orElseGet(() -> new ClaimPreview(user.isPhoneVerified(), List.of(), List.of(), List.of()));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("회원"));
    }

    private PhoneVerification findValidVerification(String phone, String verificationCode) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (String candidatePhone : candidatePhones(phone)) {
            Optional<PhoneVerification> verification = phoneVerificationRepository
                    .findByPhoneAndCodeAndVerifiedFalseAndExpiresAtAfter(
                            candidatePhone, verificationCode, now);
            if (verification.isPresent()) {
                return verification.get();
            }
        }
        throw new PhoneVerificationFailedException();
    }

    private Optional<Guest> findGuestByAnyPhoneFormat(String phone) {
        for (String candidatePhone : candidatePhones(phone)) {
            Optional<Guest> guest = guestRepository.findByPhone(candidatePhone);
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

    public record ClaimPreview(boolean phoneVerified,
                               List<ClaimOrderSummary> orders,
                               List<ClaimBookingSummary> bookings,
                               List<ClaimPassSummary> passes) {}

    public record ClaimOrderSummary(Long orderId, String status, long totalAmount, LocalDateTime createdAt) {
        static ClaimOrderSummary from(Order order) {
            return new ClaimOrderSummary(order.getId(), order.getStatus().name(),
                    order.getTotalAmount(), order.getCreatedAt());
        }
    }

    public record ClaimBookingSummary(Long bookingId, String status, String className,
                                      LocalDateTime startAt, LocalDateTime endAt) {
        static ClaimBookingSummary from(Booking booking) {
            return new ClaimBookingSummary(booking.getId(), booking.getStatus().name(),
                    booking.getBookingClass().getName(), booking.getSlot().getStartAt(),
                    booking.getSlot().getEndAt());
        }
    }

    public record ClaimPassSummary(Long passId, int remainingCredits,
                                   int totalCredits, LocalDateTime expiresAt, long totalPrice) {
        static ClaimPassSummary from(PassPurchase pass) {
            return new ClaimPassSummary(pass.getId(), pass.getRemainingCredits(),
                    pass.getTotalCredits(), pass.getExpiresAt(), pass.getTotalPrice());
        }
    }

    public record ClaimResult(int claimedOrderCount, int claimedBookingCount, int claimedPassCount) {}
}
