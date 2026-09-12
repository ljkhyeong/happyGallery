package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentTrackingRefreshTransactionService {
    private final FulfillmentPort fulfillmentPort;

    public ShipmentTrackingRefreshTransactionService(FulfillmentPort fulfillmentPort) {
        this.fulfillmentPort = fulfillmentPort;
    }

    public record Target(Long orderId, String trackingNumber) {}

    @Transactional
    public Optional<Target> claim(Long id, LocalDateTime now, LocalDateTime checkedBefore) {
        return fulfillmentPort.findByIdForUpdate(id)
                .filter(fulfillment -> fulfillment.claimTrackingRefresh(now, checkedBefore))
                .map(fulfillment -> new Target(fulfillment.getOrderId(), fulfillment.getTrackingNumber()));
    }
}
