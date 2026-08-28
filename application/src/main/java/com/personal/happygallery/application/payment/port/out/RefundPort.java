package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.booking.Refund;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundPort {

    <S extends Refund> S save(S refund);

    Optional<Refund> findById(Long id);

    Optional<Refund> findByIdForUpdate(Long id);

    Optional<Refund> findLatestByBookingId(Long bookingId);

    Optional<Refund> findDirectByOrderId(Long orderId);

    Optional<Refund> findByOrderClaimId(Long orderClaimId);

    Optional<Refund> findByPassPurchaseId(Long passPurchaseId);

    Optional<Refund> findByPaymentAttemptId(Long paymentAttemptId);

    List<Refund> findByPaymentAttemptIdIn(List<Long> paymentAttemptIds);

    List<Refund> findByPassPurchaseIdIn(List<Long> passPurchaseIds);

    List<Refund> findByOrderClaimIdIn(List<Long> orderClaimIds);

    long sumRewardRevokeAmountByOrderId(Long orderId);

    List<Refund> findActionRequired(int limit);

    List<Refund> findActionRequiredAfter(LocalDateTime createdAt, Long id, int limit);

    List<Long> findRecoverableIds(LocalDateTime now, LocalDateTime staleBefore, int limit);

    List<RefundBacklogSummary> summarizeUnresolvedBacklog();
}
