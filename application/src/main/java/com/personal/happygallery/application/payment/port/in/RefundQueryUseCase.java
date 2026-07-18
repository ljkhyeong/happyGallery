package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.booking.Refund;

public interface RefundQueryUseCase {

    Refund getRefund(Long refundId);
}
