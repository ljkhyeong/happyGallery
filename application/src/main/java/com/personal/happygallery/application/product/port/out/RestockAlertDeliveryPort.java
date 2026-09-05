package com.personal.happygallery.application.product.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RestockAlertDeliveryPort {
    List<Long> findCandidateIds(long afterId, int limit);
    Optional<Long> findEligibleUserId(Long alertId);
    Optional<LocalDateTime> findSentAt(Long alertId);
}
