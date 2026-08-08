package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.payment.port.out.RefundBacklogSummary;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaRefundPersistenceAdapter implements RefundPort {

    private final RefundRepository repository;

    JpaRefundPersistenceAdapter(RefundRepository repository) {
        this.repository = repository;
    }

    @Override
    public Refund save(Refund refund) {
        return repository.save(refund);
    }

    @Override
    public Optional<Refund> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Refund> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id);
    }

    @Override
    public Optional<Refund> findByBookingId(Long bookingId) {
        return repository.findByBookingId(bookingId);
    }

    @Override
    public Optional<Refund> findDirectByOrderId(Long orderId) {
        return repository.findDirectByOrderId(orderId);
    }

    @Override
    public Optional<Refund> findByOrderClaimId(Long orderClaimId) {
        return repository.findByOrderClaimId(orderClaimId);
    }

    @Override
    public Optional<Refund> findByPassPurchaseId(Long passPurchaseId) {
        return repository.findByPassPurchaseId(passPurchaseId);
    }

    @Override
    public Optional<Refund> findByPaymentAttemptId(Long paymentAttemptId) {
        return repository.findByPaymentAttemptId(paymentAttemptId);
    }

    @Override
    public List<Refund> findByPaymentAttemptIdIn(List<Long> paymentAttemptIds) {
        return repository.findByPaymentAttemptIdIn(paymentAttemptIds);
    }

    @Override
    public List<Refund> findByPassPurchaseIdIn(List<Long> passPurchaseIds) {
        return repository.findByPassPurchaseIdIn(passPurchaseIds);
    }

    @Override
    public List<Refund> findByOrderClaimIdIn(List<Long> orderClaimIds) {
        return repository.findByOrderClaimIdIn(orderClaimIds);
    }

    @Override
    public long sumRewardRevokeAmountByOrderId(Long orderId) {
        return repository.sumRewardRevokeAmountByOrderId(orderId);
    }

    @Override
    public List<Refund> findActionRequired(int limit) {
        return repository.findActionRequired(limit);
    }

    @Override
    public List<Refund> findActionRequiredAfter(LocalDateTime createdAt, Long id, int limit) {
        return repository.findActionRequiredAfter(createdAt, id, limit);
    }

    @Override
    public List<Long> findRecoverableIds(LocalDateTime now, LocalDateTime staleBefore, int limit) {
        return repository.findRecoverableIds(now, staleBefore, limit);
    }

    @Override
    public List<RefundBacklogSummary> summarizeUnresolvedBacklog() {
        return repository.summarizeUnresolvedBacklog();
    }
}
