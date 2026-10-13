package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.util.Objects;
import org.springframework.stereotype.Component;

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
                || !guestTokenService.matches(statusToken, attempt.getStatusAccessTokenHash())) {
            throw paymentNotFound();
        }
    }

    private NotFoundException paymentNotFound() {
        return new NotFoundException("결제");
    }
}
