package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.OrderClaim;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderClaimPort {

    <S extends OrderClaim> S save(S claim);

    Optional<Long> findOrderIdById(Long id);

    Optional<OrderClaim> findByIdForUpdate(Long id);

    List<OrderClaim> findByOrderIdOrderByRequestedAtDesc(Long orderId);

    /** 커서 값이 null이면 첫 페이지를 조회한다. 정렬은 (requestedAt DESC, id DESC)다. */
    List<OrderClaim> findRecent(LocalDateTime cursorRequestedAt, Long cursorId, int limit);

    /** 커서 값이 null이면 첫 페이지를 조회한다. 정렬은 (requestedAt DESC, id DESC)다. */
    List<OrderClaim> findRecentByStatus(
            OrderClaimStatus status, LocalDateTime cursorRequestedAt, Long cursorId, int limit);
}
