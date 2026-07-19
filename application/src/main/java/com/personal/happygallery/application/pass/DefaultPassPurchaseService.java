package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.pass.port.in.PassPurchaseUseCase;
import com.personal.happygallery.application.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.time.TimeBoundary;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPassPurchaseService implements PassPurchaseUseCase {

    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;
    private final Clock clock;

    public DefaultPassPurchaseService(PassPurchaseStorePort passPurchaseStore,
                                      PassLedgerStorePort passLedgerStore,
                                      Clock clock) {
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.clock = clock;
    }

    /** 회원 8회권 구매. prepare 단계에서 확정한 서버 가격을 저장한다. */
    @Override
    public PassPurchase purchaseForMember(Long userId, long preparedTotalPrice) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime expiresAt = TimeBoundary.passExpiresAtLocal(now);

        PassPurchase purchase = passPurchaseStore.save(
                PassPurchase.forMember(userId, expiresAt, preparedTotalPrice));
        passLedgerStore.save(new PassLedger(purchase, PassLedgerType.EARN, purchase.getTotalCredits()));

        return purchase;
    }
}
