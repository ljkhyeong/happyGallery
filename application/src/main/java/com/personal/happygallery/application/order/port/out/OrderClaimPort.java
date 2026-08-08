package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.OrderClaim;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderClaimPort {

    OrderClaim save(OrderClaim claim);

    Optional<Long> findOrderIdById(Long id);

    Optional<OrderClaim> findByIdForUpdate(Long id);

    List<OrderClaim> findByOrderIdOrderByRequestedAtDesc(Long orderId);

    List<OrderClaim> findRecent(int limit);

    List<OrderClaim> findRecentAfter(LocalDateTime requestedAt, Long id, int limit);

    List<OrderClaim> findRecentByStatus(OrderClaimStatus status, int limit);

    List<OrderClaim> findRecentByStatusAfter(
            OrderClaimStatus status, LocalDateTime requestedAt, Long id, int limit);
}
