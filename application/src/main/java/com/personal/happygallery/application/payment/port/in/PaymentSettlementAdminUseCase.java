package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.payment.PaymentSettlement;
import java.util.List;

public interface PaymentSettlementAdminUseCase {

    List<PaymentSettlement> findIssues(int limit);
}
