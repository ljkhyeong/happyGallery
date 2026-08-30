package com.personal.happygallery.application.pass.port.out;

import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PassPurchaseReaderPort {

    Optional<PassPurchase> findById(Long id);

    Optional<PassPurchase> findByIdForUpdate(Long id);

    List<PassPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId, int limit);

    List<PassPurchase> findByUserIdOrderByPurchasedAtDescAfter(
            Long userId, LocalDateTime purchasedAt, Long id, int limit);

    List<PassPurchase> findExpiredWithRemainingCreditsAfterId(
            LocalDateTime now, int credits, Long afterId, int limit);

}
