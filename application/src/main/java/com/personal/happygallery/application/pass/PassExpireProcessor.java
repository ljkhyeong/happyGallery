package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
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
    private final PassExpirationSupport expirationSupport;

    public PassExpireProcessor(PassPurchaseReaderPort passPurchaseReader,
                               PassExpirationSupport expirationSupport) {
        this.passPurchaseReader = passPurchaseReader;
        this.expirationSupport = expirationSupport;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(Long passId) {
        PassPurchase pass = passPurchaseReader.findByIdForUpdate(passId)
                .orElseThrow(NotFoundException.supplier("8회권"));
        int creditsToExpire = expirationSupport.expireIfReached(pass).orElse(0);
        if (creditsToExpire == 0) {
            return false;
        }
        log.info("Pass expired [passId={}] credits소멸={}", pass.getId(), creditsToExpire);
        return true;
    }
}
