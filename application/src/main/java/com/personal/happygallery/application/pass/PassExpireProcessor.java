package com.personal.happygallery.application.pass;

import com.personal.happygallery.domain.pass.PassPurchase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PassExpireProcessor {

    private static final Logger log = LoggerFactory.getLogger(PassExpireProcessor.class);

    private final PassCreditService passCreditService;
    private final PassExpirationSupport expirationSupport;

    PassExpireProcessor(PassCreditService passCreditService,
                        PassExpirationSupport expirationSupport) {
        this.passCreditService = passCreditService;
        this.expirationSupport = expirationSupport;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(Long passId) {
        PassPurchase pass = passCreditService.requireForUpdate(passId);
        int creditsToExpire = expirationSupport.expireIfReached(pass).orElse(0);
        if (creditsToExpire == 0) {
            return false;
        }
        log.info("Pass expired [passId={}] credits소멸={}", pass.getId(), creditsToExpire);
        return true;
    }
}
