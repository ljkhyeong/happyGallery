package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class PaymentAttemptAccessVerifier {

    private final GuestTokenService guestTokenService;

    PaymentAttemptAccessVerifier(GuestTokenService guestTokenService) {
        this.guestTokenService = guestTokenService;
    }

    void requireCustomerAccess(PaymentAttempt attempt, AuthContext auth, String statusToken) {
        if (auth == null) {
            throw paymentNotFound();
        }
        if (auth.isMember()) {
            if (Objects.equals(attempt.getOwnerUserId(), auth.userId())) {
                return;
            }
            throw paymentNotFound();
        }
        if (attempt.getOwnerUserId() != null
                || !StringUtils.hasText(statusToken)
                || !StringUtils.hasText(attempt.getStatusAccessTokenHash())) {
            throw paymentNotFound();
        }

        String resolvedHash;
        try {
            resolvedHash = guestTokenService.resolveTokenHash(statusToken);
        } catch (NotFoundException ignored) {
            throw paymentNotFound();
        }
        if (!MessageDigest.isEqual(
                resolvedHash.getBytes(StandardCharsets.UTF_8),
                attempt.getStatusAccessTokenHash().getBytes(StandardCharsets.UTF_8))) {
            throw paymentNotFound();
        }
    }

    private NotFoundException paymentNotFound() {
        return new NotFoundException("결제");
    }
}
