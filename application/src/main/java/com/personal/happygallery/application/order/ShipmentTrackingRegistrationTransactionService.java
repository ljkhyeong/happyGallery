package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider.RegistrationCommand;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider.RegistrationResult;
import com.personal.happygallery.domain.order.Fulfillment;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class ShipmentTrackingRegistrationTransactionService {

    static final int MAX_ATTEMPTS = 10;
    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    private final FulfillmentPort fulfillmentPort;

    ShipmentTrackingRegistrationTransactionService(FulfillmentPort fulfillmentPort) {
        this.fulfillmentPort = fulfillmentPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<RegistrationCommand> claim(Long fulfillmentId, LocalDateTime now) {
        Fulfillment fulfillment = fulfillmentPort.findByIdForUpdate(fulfillmentId).orElse(null);
        if (fulfillment == null
                || !fulfillment.claimTrackingRegistration(now, now.minus(PROCESSING_TIMEOUT))) {
            return Optional.empty();
        }
        fulfillmentPort.save(fulfillment);
        return Optional.of(new RegistrationCommand(
                fulfillment.getOrderId(), fulfillment.getCarrierCode(), fulfillment.getTrackingNumber()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(Long fulfillmentId, RegistrationResult result, LocalDateTime now) {
        Fulfillment fulfillment = fulfillmentPort.findByIdForUpdate(fulfillmentId).orElse(null);
        if (fulfillment == null) {
            return;
        }
        if (result.success()) {
            fulfillment.completeTrackingRegistration(result.requestId(), now);
        } else {
            LocalDateTime retryAt = result.retryable()
                    ? now.plusMinutes(retryDelayMinutes(fulfillment.getTrackingRegistrationAttempts()))
                    : null;
            fulfillment.failTrackingRegistration(result.reason(), retryAt, MAX_ATTEMPTS);
        }
        fulfillmentPort.save(fulfillment);
    }

    private static long retryDelayMinutes(int attempts) {
        return Math.min(60, 1L << Math.min(attempts - 1, 6));
    }
}
