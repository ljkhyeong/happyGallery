package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 잠긴 8회권의 만료 여부를 확인하고 잔액과 원장을 같은 트랜잭션에서 정리한다. */
@Component
class PassExpirationSupport {

    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;
    private final Clock clock;

    PassExpirationSupport(PassPurchaseStorePort passPurchaseStore,
                          PassLedgerStorePort passLedgerStore,
                          Clock clock) {
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ExpirationResult expireIfReached(PassPurchase pass) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!pass.isExpiredAt(now)) {
            return new ExpirationResult(false, 0);
        }

        int expiredCredits = pass.expireIfReached(now);
        if (expiredCredits > 0) {
            passLedgerStore.save(new PassLedger(pass, PassLedgerType.EXPIRE, expiredCredits));
            passPurchaseStore.save(pass);
        }
        return new ExpirationResult(true, expiredCredits);
    }

    record ExpirationResult(boolean expired, int expiredCredits) {}
}
