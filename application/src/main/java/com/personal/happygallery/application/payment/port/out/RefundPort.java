package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.booking.Refund;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundPort {

    Refund save(Refund refund);

    Optional<Refund> findById(Long id);

    Optional<Refund> findByIdForUpdate(Long id);

    Optional<Refund> findByBookingId(Long bookingId);

    Optional<Refund> findByOrderId(Long orderId);

    Optional<Refund> findByPassPurchaseId(Long passPurchaseId);

    Optional<Refund> findByPaymentAttemptId(Long paymentAttemptId);

    List<Refund> findByPassPurchaseIdIn(List<Long> passPurchaseIds);

    List<Refund> findActionRequired(int limit);

    List<Refund> findActionRequiredAfter(LocalDateTime createdAt, Long id, int limit);

    List<Long> findRecoverableIds(LocalDateTime now, LocalDateTime staleBefore, int limit);

    List<RefundBacklogSummary> summarizeUnresolvedBacklog();
}
