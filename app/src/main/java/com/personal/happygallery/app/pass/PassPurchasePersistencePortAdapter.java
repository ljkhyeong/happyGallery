package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link PassPurchaseRepository}(infra) → {@link PassPurchaseReaderPort} + {@link PassPurchaseStorePort}(app) 브릿지 어댑터.
 */
@Component
class PassPurchasePersistencePortAdapter implements PassPurchaseReaderPort, PassPurchaseStorePort {

    private final PassPurchaseRepository passPurchaseRepository;

    PassPurchasePersistencePortAdapter(PassPurchaseRepository passPurchaseRepository) {
        this.passPurchaseRepository = passPurchaseRepository;
    }

    @Override
    public Optional<PassPurchase> findById(Long id) {
        return passPurchaseRepository.findById(id);
    }

    @Override
    public List<PassPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId) {
        return passPurchaseRepository.findByUserIdOrderByPurchasedAtDesc(userId);
    }

    @Override
    public List<PassPurchase> findByGuestIdOrderByPurchasedAtDesc(Long guestId) {
        return passPurchaseRepository.findByGuestIdOrderByPurchasedAtDesc(guestId);
    }

    @Override
    public List<PassPurchase> findByExpiresAtBeforeAndRemainingCreditsGreaterThan(LocalDateTime now, int credits) {
        return passPurchaseRepository.findByExpiresAtBeforeAndRemainingCreditsGreaterThan(now, credits);
    }

    @Override
    public List<PassPurchase> findByExpiresAtBetweenAndRemainingCreditsGreaterThan(
            LocalDateTime start, LocalDateTime end, int credits) {
        return passPurchaseRepository.findByExpiresAtBetweenAndRemainingCreditsGreaterThan(start, end, credits);
    }

    @Override
    public List<PassPurchase> findExpiringWithGuestBetween(LocalDateTime start, LocalDateTime end) {
        return passPurchaseRepository.findExpiringWithGuestBetween(start, end);
    }

    @Override
    public PassPurchase save(PassPurchase passPurchase) {
        return passPurchaseRepository.save(passPurchase);
    }
}
