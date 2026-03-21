package com.personal.happygallery.infra.pass;

import com.personal.happygallery.app.pass.port.out.PassLedgerReaderPort;
import com.personal.happygallery.app.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.domain.pass.PassLedger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassLedgerRepository extends JpaRepository<PassLedger, Long>, PassLedgerReaderPort, PassLedgerStorePort {

    @Override PassLedger save(PassLedger ledger);

    List<PassLedger> findByPassPurchaseId(Long passPurchaseId);
}
