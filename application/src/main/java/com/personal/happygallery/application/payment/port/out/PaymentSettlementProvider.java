package com.personal.happygallery.application.payment.port.out;

import java.time.LocalDate;
import java.util.List;

public interface PaymentSettlementProvider {

    List<PaymentSettlementItem> findSettlements(LocalDate startDate, LocalDate endDate);
}
