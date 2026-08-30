package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentSettlementItem;
import com.personal.happygallery.application.payment.port.out.PaymentSettlementProvider;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
class FakePaymentSettlementProvider implements PaymentSettlementProvider {

    @Override
    public List<PaymentSettlementItem> findSettlements(LocalDate startDate, LocalDate endDate) {
        return List.of();
    }
}
