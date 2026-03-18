package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.customer.VerifiedGuestResolver;
import com.personal.happygallery.app.customer.port.out.GuestReaderPort;
import com.personal.happygallery.app.pass.port.in.PassPurchaseUseCase;
import com.personal.happygallery.app.pass.port.out.PassLedgerStorePort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Guest;
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

    private final VerifiedGuestResolver verifiedGuestResolver;
    private final GuestReaderPort guestReader;
    private final PassPurchaseStorePort passPurchaseStore;
    private final PassLedgerStorePort passLedgerStore;
    private final Clock clock;

    public DefaultPassPurchaseService(VerifiedGuestResolver verifiedGuestResolver,
                               GuestReaderPort guestReader,
                               PassPurchaseStorePort passPurchaseStore,
                               PassLedgerStorePort passLedgerStore,
                               Clock clock) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.guestReader = guestReader;
        this.passPurchaseStore = passPurchaseStore;
        this.passLedgerStore = passLedgerStore;
        this.clock = clock;
    }

    /**
     * 게스트 8회권 구매 (guestId 직접 지정).
     *
     * @param totalPrice 8회권 총 결제금액 (KRW) — 정산 환불 계산 기준
     */
    public PassPurchase purchaseForGuest(Long guestId, long totalPrice) {
        Guest guest = guestReader.findById(guestId)
                .orElseThrow(() -> new NotFoundException("게스트"));
        return createPurchase(guest, totalPrice);
    }

    /**
     * 휴대폰 인증 기반 8회권 구매.
     *
     * <ol>
     *   <li>인증 코드 검증 + 소모</li>
     *   <li>Guest upsert (전화번호 기준)</li>
     *   <li>PassPurchase 저장 (totalPrice 기록)</li>
     *   <li>EARN ledger 기록 (amount = 8)</li>
     * </ol>
     */
    public PassPurchase purchaseByPhone(String phone, String verificationCode,
                                        String name, long totalPrice) {
        Guest guest = verifiedGuestResolver.resolveVerifiedGuest(phone, verificationCode, name);
        return createPurchase(guest, totalPrice);
    }

    /**
     * 회원 8회권 구매.
     */
    public PassPurchase purchaseForMember(Long userId, long totalPrice) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime expiresAt = TimeBoundary.passExpiresAtLocal(now);

        PassPurchase purchase = passPurchaseStore.save(PassPurchase.forMember(userId, expiresAt, totalPrice));
        passLedgerStore.save(new PassLedger(purchase, PassLedgerType.EARN, purchase.getTotalCredits()));

        return purchase;
    }

    private PassPurchase createPurchase(Guest guest, long totalPrice) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime expiresAt = TimeBoundary.passExpiresAtLocal(now);

        PassPurchase purchase = passPurchaseStore.save(PassPurchase.forGuest(guest, expiresAt, totalPrice));
        passLedgerStore.save(new PassLedger(purchase, PassLedgerType.EARN, purchase.getTotalCredits()));

        return purchase;
    }
}
