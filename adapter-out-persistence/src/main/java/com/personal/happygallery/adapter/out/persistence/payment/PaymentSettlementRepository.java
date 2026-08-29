package com.personal.happygallery.adapter.out.persistence.payment;

import com.personal.happygallery.application.payment.port.out.PaymentSettlementPort;
import com.personal.happygallery.domain.payment.PaymentSettlement;
import com.personal.happygallery.domain.payment.PaymentSettlementStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentSettlementRepository
        extends JpaRepository<PaymentSettlement, Long>, PaymentSettlementPort {

    @Override
    Optional<PaymentSettlement> findByTransactionKey(String transactionKey);

    @Override
    <S extends PaymentSettlement> S save(S settlement);

    @Query("""
            SELECT settlement
            FROM PaymentSettlement settlement
            WHERE settlement.reconciliationStatus <> :matchedStatus
            ORDER BY settlement.soldDate DESC, settlement.id DESC
            """)
    List<PaymentSettlement> findIssuesPage(
            PaymentSettlementStatus matchedStatus, Pageable pageable);

    @Override
    default List<PaymentSettlement> findIssues(int limit) {
        return findIssuesPage(PaymentSettlementStatus.MATCHED, PageRequest.ofSize(limit));
    }
}
