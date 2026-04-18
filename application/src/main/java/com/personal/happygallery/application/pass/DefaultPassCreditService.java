package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.pass.port.in.PassCreditUseCase;
import com.personal.happygallery.application.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultPassCreditService implements PassCreditUseCase {

    private final PassPurchaseReaderPort passPurchaseReader;
    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;
    private final Clock clock;

    DefaultPassCreditService(PassPurchaseReaderPort passPurchaseReader,
                             PassPurchaseStorePort passPurchaseStore,
                             PassLedgerStorePort passLedgerStore,
                             Clock clock) {
        this.passPurchaseReader = passPurchaseReader;
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PassPurchase deductCredit(Long passId, Long ownerUserId) {
        PassPurchase pass = passPurchaseReader.findById(passId)
                .orElseThrow(NotFoundException.supplier("8회권"));

        if (ownerUserId != null && !Objects.equals(pass.getUserId(), ownerUserId)) {
            throw new NotFoundException("8회권");
        }

        pass.requireUsable(LocalDateTime.now(clock));
        passLedgerStore.save(new PassLedger(pass, PassLedgerType.USE, 1));
        pass.useCredit();
        passPurchaseStore.save(pass);
        return pass;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreCredit(Long passId, Long bookingId) {
        PassPurchase pass = passPurchaseReader.findById(passId)
                .orElseThrow(NotFoundException.supplier("8회권"));
        passLedgerStore.save(
                new PassLedger(pass, PassLedgerType.REFUND, 1, bookingId));
        pass.refundCredit();
        passPurchaseStore.save(pass);
    }
}
