package com.personal.happygallery.adapter.out.persistence.pass;

import com.personal.happygallery.application.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.domain.pass.PassLedger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassLedgerRepository extends JpaRepository<PassLedger, Long>,
        PassLedgerStorePort {

    @Override
    <S extends PassLedger> S save(S ledger);

    /** 이용권 원장 조회 — 테스트 검증용 */
    List<PassLedger> findByPassPurchaseId(Long passPurchaseId);
}
