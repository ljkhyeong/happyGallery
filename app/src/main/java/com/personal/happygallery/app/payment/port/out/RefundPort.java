package com.personal.happygallery.app.payment.port.out;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.RefundStatus;
import java.util.List;
import java.util.Optional;

public interface RefundPort {

    Refund save(Refund refund);

    Optional<Refund> findById(Long id);

    List<Refund> findByStatus(RefundStatus status);
}
