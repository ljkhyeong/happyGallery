package com.personal.happygallery.application.pass;

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
class DefaultPassCreditService implements PassCreditService {

    private final PassPurchaseReaderPort passPurchaseReader;
    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;
    private final PassExpirationSupport expirationSupport;
    private final Clock clock;

    DefaultPassCreditService(PassPurchaseReaderPort passPurchaseReader,
                             PassPurchaseStorePort passPurchaseStore,
                             PassLedgerStorePort passLedgerStore,
                             PassExpirationSupport expirationSupport,
                             Clock clock) {
        this.passPurchaseReader = passPurchaseReader;
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.expirationSupport = expirationSupport;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PassPurchase requireOwnedForUpdate(Long passId, Long ownerUserId) {
        PassPurchase pass = requireForUpdate(passId);

        if (ownerUserId == null || !Objects.equals(pass.getUserId(), ownerUserId)) {
            throw new NotFoundException("8회권");
        }
        return pass;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PassPurchase deductCredit(PassPurchase pass, Long bookingId) {
        pass.useCredit(LocalDateTime.now(clock));
        passLedgerStore.save(new PassLedger(pass, PassLedgerType.USE, 1, bookingId));
        passPurchaseStore.save(pass);
        return pass;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public PassPurchase requireForUpdate(Long passId) {
        return passPurchaseReader.findByIdForUpdate(passId)
                .orElseThrow(NotFoundException.supplier("8회권"));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean restoreCredit(PassPurchase pass, Long bookingId) {
        if (expirationSupport.expireIfReached(pass).isPresent()) {
            return false;
        }
        passLedgerStore.save(
                new PassLedger(pass, PassLedgerType.REFUND, 1, bookingId));
        pass.refundCredit();
        passPurchaseStore.save(pass);
        return true;
    }
}
