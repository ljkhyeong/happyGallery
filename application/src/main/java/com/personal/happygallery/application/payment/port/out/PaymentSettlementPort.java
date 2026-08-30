package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.PaymentSettlement;
import java.util.List;
import java.util.Optional;

public interface PaymentSettlementPort {

    Optional<PaymentSettlement> findByTransactionKey(String transactionKey);

    <S extends PaymentSettlement> S save(S settlement);

    List<PaymentSettlement> findIssues(int limit);
}
