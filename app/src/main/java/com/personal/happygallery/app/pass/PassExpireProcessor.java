package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassExpireProcessor {

    private static final Logger log = LoggerFactory.getLogger(PassExpireProcessor.class);

    private final PassPurchaseReaderPort passPurchaseReader;
    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;

    public PassExpireProcessor(PassPurchaseReaderPort passPurchaseReader,
                               PassPurchaseStorePort passPurchaseStore,
                               PassLedgerStorePort passLedgerStore) {
        this.passPurchaseReader = passPurchaseReader;
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(Long passId) {
        PassPurchase pass = passPurchaseReader.findById(passId)
                .orElseThrow(() -> new NotFoundException("8회권"));
        if (!pass.hasRemainingCredits()) {
            return false;
        }

        int creditsToExpire = pass.getRemainingCredits();
        passLedgerStore.save(new PassLedger(pass, PassLedgerType.EXPIRE, creditsToExpire));
        pass.expire();
        passPurchaseStore.save(pass);
        log.info("Pass expired [passId={}] credits소멸={}", pass.getId(), creditsToExpire);
        return true;
    }
}
