package com.personal.happygallery.adapter.out.persistence.pass;

import com.personal.happygallery.application.pass.port.out.PassLedgerReaderPort;
import com.personal.happygallery.application.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.domain.pass.PassLedger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassLedgerRepository extends JpaRepository<PassLedger, Long>,
        PassLedgerReaderPort,
        PassLedgerStorePort {

    @Override
    <S extends PassLedger> S save(S ledger);

    @Override List<PassLedger> findByPassPurchaseId(Long passPurchaseId);
}
