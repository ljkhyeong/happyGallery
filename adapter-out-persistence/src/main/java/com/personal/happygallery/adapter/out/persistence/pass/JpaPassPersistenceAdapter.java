package com.personal.happygallery.adapter.out.persistence.pass;

import com.personal.happygallery.application.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassPurchase;
import org.springframework.stereotype.Repository;

@Repository
class JpaPassPersistenceAdapter implements PassLedgerStorePort, PassPurchaseStorePort {

    private final PassLedgerRepository passLedgerRepository;
    private final PassPurchaseRepository passPurchaseRepository;

    JpaPassPersistenceAdapter(
            PassLedgerRepository passLedgerRepository,
            PassPurchaseRepository passPurchaseRepository) {
        this.passLedgerRepository = passLedgerRepository;
        this.passPurchaseRepository = passPurchaseRepository;
    }

    @Override
    public PassLedger save(PassLedger ledger) {
        return passLedgerRepository.save(ledger);
    }

    @Override
    public PassPurchase save(PassPurchase passPurchase) {
        return passPurchaseRepository.save(passPurchase);
    }
}
