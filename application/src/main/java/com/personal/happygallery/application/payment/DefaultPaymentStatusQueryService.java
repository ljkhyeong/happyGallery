package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultPaymentStatusQueryService implements PaymentStatusQueryUseCase {

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptAccessVerifier accessVerifier;
    private final CompletedGuestAccessTokenResolver accessTokenResolver;
    private final CustomerPaymentStatusResolver statusResolver;

    public DefaultPaymentStatusQueryService(PaymentAttemptReaderPort attemptReader,
                                            PaymentAttemptAccessVerifier accessVerifier,
                                            CompletedGuestAccessTokenResolver accessTokenResolver,
                                            CustomerPaymentStatusResolver statusResolver) {
        this.attemptReader = attemptReader;
        this.accessVerifier = accessVerifier;
        this.accessTokenResolver = accessTokenResolver;
        this.statusResolver = statusResolver;
    }

    @Override
    public PaymentStatusResult getStatus(String orderId, AuthContext auth, String statusToken) {
        PaymentAttempt attempt = attemptReader.findByOrderIdExternal(orderId)
                .orElseThrow(() -> new NotFoundException("결제"));
        accessVerifier.requireCustomerAccess(attempt, auth, statusToken);

        CustomerPaymentStatus status = statusResolver.resolve(attempt);
        CompletedGuestAccessTokenResolver.ResolvedAccess access = status == CustomerPaymentStatus.COMPLETED
                ? accessTokenResolver.resolve(attempt)
                : CompletedGuestAccessTokenResolver.ResolvedAccess.none();
        return new PaymentStatusResult(
                attempt.getContext(),
                attempt.getAmount(),
                status,
                status == CustomerPaymentStatus.COMPLETED ? attempt.getFulfilledDomainId() : null,
                access.accessToken(),
                access.recoveryRequired());
    }

}
