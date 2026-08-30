package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.RefundQueryUseCase;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultRefundQueryService implements RefundQueryUseCase {

    private final RefundPort refundPort;

    public DefaultRefundQueryService(RefundPort refundPort) {
        this.refundPort = refundPort;
    }

    @Override
    public Refund getRefund(Long refundId) {
        return refundPort.findById(refundId)
                .orElseThrow(NotFoundException.supplier("환불"));
    }
}
