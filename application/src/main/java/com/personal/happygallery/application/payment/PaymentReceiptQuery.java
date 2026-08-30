package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.out.PaymentReceiptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentReceiptReaderPort.PaymentReceipt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toMap;

/** 소유권 확인이 끝난 거래의 영수증만 결제 이력에서 조회한다. */
@Component
public class PaymentReceiptQuery {

    private final PaymentReceiptReaderPort receiptReader;

    public PaymentReceiptQuery(PaymentReceiptReaderPort receiptReader) {
        this.receiptReader = receiptReader;
    }

    public String findReceipt(PaymentContext context, Long domainId) {
        return findReceipts(context, List.of(domainId)).get(domainId);
    }

    public Map<Long, String> findReceipts(PaymentContext context, List<Long> domainIds) {
        if (domainIds.isEmpty()) {
            return Map.of();
        }
        return receiptReader.findReceipts(context, domainIds).stream()
                .collect(toMap(PaymentReceipt::domainId, PaymentReceipt::receiptUrl));
    }
}
