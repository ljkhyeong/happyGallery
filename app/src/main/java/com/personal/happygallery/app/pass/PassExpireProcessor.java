package com.personal.happygallery.app.pass;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassExpireProcessor {

    private static final Logger log = LoggerFactory.getLogger(PassExpireProcessor.class);

    private final PassPurchaseRepository passPurchaseRepository;
    private final PassLedgerRepository passLedgerRepository;

    public PassExpireProcessor(PassPurchaseRepository passPurchaseRepository,
                               PassLedgerRepository passLedgerRepository) {
        this.passPurchaseRepository = passPurchaseRepository;
        this.passLedgerRepository = passLedgerRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(Long passId) {
        PassPurchase pass = passPurchaseRepository.findById(passId)
                .orElseThrow(() -> new NotFoundException("8회권"));
        if (pass.getRemainingCredits() <= 0) {
            return false;
        }

        int creditsToExpire = pass.getRemainingCredits();
        passLedgerRepository.save(new PassLedger(pass, PassLedgerType.EXPIRE, creditsToExpire));
        pass.expire();
        passPurchaseRepository.save(pass);
        log.info("Pass expired [passId={}] credits소멸={}", pass.getId(), creditsToExpire);
        return true;
    }
}
