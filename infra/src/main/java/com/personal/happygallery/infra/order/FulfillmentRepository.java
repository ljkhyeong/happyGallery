package com.personal.happygallery.infra.order;

import com.personal.happygallery.domain.order.Fulfillment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {

    Optional<Fulfillment> findByOrderId(Long orderId);
}
