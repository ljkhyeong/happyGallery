package com.personal.happygallery.app.payment;

import com.personal.happygallery.app.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.RefundStatus;
import com.personal.happygallery.infra.booking.RefundRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link RefundRepository}(infra) → {@link RefundPort}(app) 브릿지 어댑터.
 */
@Component
class RefundPortAdapter implements RefundPort {

    private final RefundRepository refundRepository;

    RefundPortAdapter(RefundRepository refundRepository) {
        this.refundRepository = refundRepository;
    }

    @Override
    public Refund save(Refund refund) {
        return refundRepository.save(refund);
    }

    @Override
    public Optional<Refund> findById(Long id) {
        return refundRepository.findById(id);
    }

    @Override
    public List<Refund> findByStatus(RefundStatus status) {
        return refundRepository.findByStatus(status);
    }
}
