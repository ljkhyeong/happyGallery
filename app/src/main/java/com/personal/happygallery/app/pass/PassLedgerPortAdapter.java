package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.pass.port.out.PassLedgerReaderPort;
import com.personal.happygallery.app.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link PassLedgerRepository}(infra) → {@link PassLedgerReaderPort} + {@link PassLedgerStorePort}(app) 브릿지 어댑터.
 */
@Component
class PassLedgerPortAdapter implements PassLedgerReaderPort, PassLedgerStorePort {

    private final PassLedgerRepository passLedgerRepository;

    PassLedgerPortAdapter(PassLedgerRepository passLedgerRepository) {
        this.passLedgerRepository = passLedgerRepository;
    }

    @Override
    public List<PassLedger> findByPassPurchaseId(Long passPurchaseId) {
        return passLedgerRepository.findByPassPurchaseId(passPurchaseId);
    }

    @Override
    public PassLedger save(PassLedger ledger) {
        return passLedgerRepository.save(ledger);
    }
}
