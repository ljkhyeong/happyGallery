package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link FulfillmentRepository}(infra) → {@link FulfillmentPort}(app) 브릿지 어댑터.
 */
@Component
class FulfillmentPortAdapter implements FulfillmentPort {

    private final FulfillmentRepository fulfillmentRepository;

    FulfillmentPortAdapter(FulfillmentRepository fulfillmentRepository) {
        this.fulfillmentRepository = fulfillmentRepository;
    }

    @Override
    public Fulfillment save(Fulfillment fulfillment) {
        return fulfillmentRepository.save(fulfillment);
    }

    @Override
    public Optional<Fulfillment> findByOrderId(Long orderId) {
        return fulfillmentRepository.findByOrderId(orderId);
    }

    @Override
    public List<Fulfillment> findExpiredPickups(LocalDateTime now) {
        return fulfillmentRepository.findExpiredPickups(now);
    }
}
