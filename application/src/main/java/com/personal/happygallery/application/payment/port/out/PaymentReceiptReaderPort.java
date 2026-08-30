package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.PaymentContext;
import java.util.List;

public interface PaymentReceiptReaderPort {

    List<PaymentReceipt> findReceipts(PaymentContext context, List<Long> domainIds);

    record PaymentReceipt(Long domainId, String receiptUrl) {}
}
