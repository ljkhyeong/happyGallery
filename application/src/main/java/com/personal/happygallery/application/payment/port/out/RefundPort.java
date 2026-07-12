package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundPort {

    Refund save(Refund refund);

    Optional<Refund> findById(Long id);

    Optional<Refund> findByIdForUpdate(Long id);

    List<Refund> findAll();

    long count();

    List<Refund> findByStatus(RefundStatus status);

    List<Refund> findByStatusIn(List<RefundStatus> statuses);

    List<Long> findRecoverableIds(LocalDateTime now, LocalDateTime staleBefore, int limit);
}
