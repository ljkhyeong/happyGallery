package com.personal.happygallery.app.pass.port.out;

import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface PassPurchaseReaderPort {

    Optional<PassPurchase> findById(Long id);

    List<PassPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    List<PassPurchase> findByExpiresAtBeforeAndRemainingCreditsGreaterThan(LocalDateTime now, int credits);
    List<PassPurchase> findByExpiresAtBeforeAndRemainingCreditsGreaterThan(LocalDateTime now, int credits, Pageable pageable);

    List<PassPurchase> findByExpiresAtBetweenAndRemainingCreditsGreaterThan(
            LocalDateTime start, LocalDateTime end, int credits);
}
