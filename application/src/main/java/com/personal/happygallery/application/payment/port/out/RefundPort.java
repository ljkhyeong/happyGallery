package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.util.List;
import java.util.Optional;

public interface RefundPort {

    Refund save(Refund refund);

    Optional<Refund> findById(Long id);

    List<Refund> findAll();

    long count();

    List<Refund> findByStatus(RefundStatus status);
}
