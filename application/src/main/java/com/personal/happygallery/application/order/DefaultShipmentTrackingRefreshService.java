package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingRefreshUseCase;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.KoreaPostTrackingLookup;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultShipmentTrackingRefreshService implements ShipmentTrackingRefreshUseCase {
    private static final int BATCH_SIZE = 100;
    private final FulfillmentPort fulfillmentPort;
    private final KoreaPostTrackingLookup lookup;
    private final ShipmentTrackingRefreshTransactionService transactionService;
    private final ShipmentTrackingWebhookUseCase trackingUpdates;
    private final Clock clock;

    public DefaultShipmentTrackingRefreshService(FulfillmentPort fulfillmentPort,
            KoreaPostTrackingLookup lookup, ShipmentTrackingRefreshTransactionService transactionService,
            ShipmentTrackingWebhookUseCase trackingUpdates, Clock clock) {
        this.fulfillmentPort = fulfillmentPort;
        this.lookup = lookup;
        this.transactionService = transactionService;
        this.trackingUpdates = trackingUpdates;
        this.clock = clock;
    }

    @Override
    public BatchResult refreshShipments() {
        if (!lookup.isEnabled()) {
            return BatchResult.successOnly(0);
        }
        return BatchExecutor.execute(
                fulfillmentPort.findTrackingRefreshCandidateIds(LocalDateTime.now(clock).minusMinutes(30), BATCH_SIZE),
                id -> id,
                this::refresh,
                "우체국 배송조회");
    }

    private boolean refresh(Long fulfillmentId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return transactionService.claim(fulfillmentId, now, now.minusMinutes(30))
                .flatMap(target -> lookup.lookup(target.orderId(), target.trackingNumber()))
                .map(update -> {
                    trackingUpdates.apply(List.of(update));
                    return true;
                }).orElse(false);
    }
}
