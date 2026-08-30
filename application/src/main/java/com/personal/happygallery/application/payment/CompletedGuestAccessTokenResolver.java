package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class CompletedGuestAccessTokenResolver {

    private static final Logger log = LoggerFactory.getLogger(CompletedGuestAccessTokenResolver.class);

    private final OrderReaderPort orderReader;
    private final BookingReaderPort bookingReader;
    private final GuestTokenService guestTokenService;
    private final FieldEncryptor fieldEncryptor;

    CompletedGuestAccessTokenResolver(OrderReaderPort orderReader,
                                      BookingReaderPort bookingReader,
                                      GuestTokenService guestTokenService,
                                      FieldEncryptor fieldEncryptor) {
        this.orderReader = orderReader;
        this.bookingReader = bookingReader;
        this.guestTokenService = guestTokenService;
        this.fieldEncryptor = fieldEncryptor;
    }

    ResolvedAccess resolve(PaymentAttempt attempt) {
        if (attempt.getOwnerUserId() != null || attempt.getContext() == PaymentContext.PASS) {
            return ResolvedAccess.none();
        }
        if (attempt.getFulfilledDomainId() == null
                || !StringUtils.hasText(attempt.getFulfilledAccessTokenEnc())) {
            return ResolvedAccess.requiresRecovery();
        }

        try {
            String rawToken = fieldEncryptor.decrypt(attempt.getFulfilledAccessTokenEnc());
            String tokenHash = guestTokenService.resolveTokenHash(rawToken);
            if (matchesCurrentGuestDomain(attempt, tokenHash)) {
                return new ResolvedAccess(rawToken, false);
            }
        } catch (NotFoundException ignored) {
            // 만료되거나 서명이 맞지 않는 토큰은 휴대폰 재인증으로 복구한다.
        } catch (RuntimeException exception) {
            log.warn("완료 결제 접근 토큰 복구 실패 [attemptId={}, type={}]",
                    attempt.getId(), exception.getClass().getSimpleName());
        }
        return ResolvedAccess.requiresRecovery();
    }

    private boolean matchesCurrentGuestDomain(PaymentAttempt attempt, String tokenHash) {
        return switch (attempt.getContext()) {
            case ORDER -> orderReader.findById(attempt.getFulfilledDomainId())
                    .filter(order -> isCurrentGuestOrder(order, tokenHash))
                    .isPresent();
            case BOOKING -> bookingReader.findById(attempt.getFulfilledDomainId())
                    .filter(booking -> isCurrentGuestBooking(booking, tokenHash))
                    .isPresent();
            case PASS -> false;
        };
    }

    private boolean isCurrentGuestOrder(Order order, String tokenHash) {
        return order.getUserId() == null
                && order.getGuestId() != null
                && hashesMatch(tokenHash, order.getAccessToken());
    }

    private boolean isCurrentGuestBooking(Booking booking, String tokenHash) {
        return booking.getUserId() == null
                && booking.getGuest() != null
                && hashesMatch(tokenHash, booking.getAccessToken());
    }

    private boolean hashesMatch(String expected, String actual) {
        return StringUtils.hasText(actual) && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    record ResolvedAccess(String accessToken, boolean recoveryRequired) {

        static ResolvedAccess none() {
            return new ResolvedAccess(null, false);
        }

        static ResolvedAccess requiresRecovery() {
            return new ResolvedAccess(null, true);
        }
    }
}
