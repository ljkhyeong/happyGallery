package com.personal.happygallery.app.pass;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.time.Clocks;
import com.personal.happygallery.common.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PassPurchaseService {

    private final GuestRepository guestRepository;
    private final PassPurchaseRepository passPurchaseRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final Clock clock;

    public PassPurchaseService(GuestRepository guestRepository,
                               PassPurchaseRepository passPurchaseRepository,
                               PassLedgerRepository passLedgerRepository,
                               Clock clock) {
        this.guestRepository = guestRepository;
        this.passPurchaseRepository = passPurchaseRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.clock = clock;
    }

    /**
     * 게스트 8회권 구매.
     *
     * <ol>
     *   <li>Guest 조회</li>
     *   <li>expires_at = now + 90일</li>
     *   <li>PassPurchase 저장</li>
     *   <li>EARN ledger 기록 (amount = 8)</li>
     * </ol>
     */
    /**
     * 게스트 8회권 구매.
     *
     * <ol>
     *   <li>Guest 조회</li>
     *   <li>expires_at = now + 90일</li>
     *   <li>PassPurchase 저장 (totalPrice 기록)</li>
     *   <li>EARN ledger 기록 (amount = 8)</li>
     * </ol>
     *
     * @param totalPrice 8회권 총 결제금액 (KRW) — 정산 환불 계산 기준
     */
    public PassPurchase purchaseForGuest(Long guestId, long totalPrice) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new NotFoundException("게스트"));

        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime expiresAt = TimeBoundary.passExpiresAtLocal(now);

        PassPurchase purchase = passPurchaseRepository.save(new PassPurchase(guest, expiresAt, totalPrice));
        passLedgerRepository.save(new PassLedger(purchase, PassLedgerType.EARN, purchase.getTotalCredits()));

        return purchase;
    }
}
