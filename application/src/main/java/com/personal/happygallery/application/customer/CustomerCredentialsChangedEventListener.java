package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.CustomerSessionRevocationPort;
import com.personal.happygallery.application.monitoring.AppMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class CustomerCredentialsChangedEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(CustomerCredentialsChangedEventListener.class);

    private final CustomerSessionRevocationPort sessionRevocation;
    private final AppMetrics metrics;

    CustomerCredentialsChangedEventListener(CustomerSessionRevocationPort sessionRevocation,
                                            AppMetrics metrics) {
        this.sessionRevocation = sessionRevocation;
        this.metrics = metrics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void revokeSessions(CustomerCredentialsChangedEvent event) {
        try {
            sessionRevocation.revokeCredentialVersion(
                    event.userId(), event.invalidatedCredentialVersion());
        } catch (RuntimeException exception) {
            metrics.incrementCustomerSessionRevocationFailure();
            log.error("회원 자격 증명 변경 후 세션 폐기 실패 [userId={} type={}]",
                    event.userId(), exception.getClass().getSimpleName(), exception);
        }
    }
}

record CustomerCredentialsChangedEvent(Long userId, long invalidatedCredentialVersion) {
}
