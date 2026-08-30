package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingRegistrationUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider.RegistrationCommand;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider.RegistrationResult;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultShipmentTrackingRegistrationService implements ShipmentTrackingRegistrationUseCase {

    private static final int BATCH_SIZE = 100;

    private final FulfillmentPort fulfillmentPort;
    private final ShipmentTrackingProvider trackingProvider;
    private final ShipmentTrackingRegistrationTransactionService transactionService;
    private final Clock clock;

    public DefaultShipmentTrackingRegistrationService(
            FulfillmentPort fulfillmentPort,
            ShipmentTrackingProvider trackingProvider,
            ShipmentTrackingRegistrationTransactionService transactionService,
            Clock clock) {
        this.fulfillmentPort = fulfillmentPort;
        this.trackingProvider = trackingProvider;
        this.transactionService = transactionService;
        this.clock = clock;
    }

    @Override
    public BatchResult registerPendingShipments() {
        if (!trackingProvider.isEnabled()) {
            return BatchResult.successOnly(0);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return BatchExecutor.execute(
                fulfillmentPort.findTrackingRegistrationCandidateIds(
                        now, now.minusMinutes(5), BATCH_SIZE),
                id -> id,
                this::register,
                "배송조회 등록");
    }

    private boolean register(Long fulfillmentId) {
        LocalDateTime claimedAt = LocalDateTime.now(clock);
        Optional<RegistrationCommand> claimed = transactionService.claim(fulfillmentId, claimedAt);
        if (claimed.isEmpty()) {
            return false;
        }
        RegistrationResult result = trackingProvider.register(claimed.get());
        transactionService.finish(fulfillmentId, result, LocalDateTime.now(clock));
        if (!result.success()) {
            throw new ShipmentTrackingRegistrationException(result.reason());
        }
        return true;
    }

    private static final class ShipmentTrackingRegistrationException extends RuntimeException {
        private ShipmentTrackingRegistrationException(String message) {
            super(message);
        }
    }
}
