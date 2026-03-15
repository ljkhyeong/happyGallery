package com.personal.happygallery.app.pass;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.error.PhoneVerificationFailedException;
import com.personal.happygallery.common.time.TimeBoundary;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
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
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PassPurchaseRepository passPurchaseRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final Clock clock;

    public PassPurchaseService(GuestRepository guestRepository,
                               PhoneVerificationRepository phoneVerificationRepository,
                               PassPurchaseRepository passPurchaseRepository,
                               PassLedgerRepository passLedgerRepository,
                               Clock clock) {
        this.guestRepository = guestRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.passPurchaseRepository = passPurchaseRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.clock = clock;
    }

    /**
     * 게스트 8회권 구매 (guestId 직접 지정).
     *
     * @param totalPrice 8회권 총 결제금액 (KRW) — 정산 환불 계산 기준
     */
    public PassPurchase purchaseForGuest(Long guestId, long totalPrice) {
        Guest guest = guestRepository.findById(guestId)
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
        // 인증 코드 검증
        PhoneVerification pv = phoneVerificationRepository
                .findByPhoneAndCodeAndVerifiedFalseAndExpiresAtAfter(
                        phone, verificationCode, LocalDateTime.now(clock))
                .orElseThrow(PhoneVerificationFailedException::new);
        pv.markVerified();

        // Guest upsert
        Guest guest = guestRepository.findByPhone(phone)
                .orElseGet(() -> guestRepository.save(new Guest(name, phone)));
        guest.markPhoneVerified();

        return createPurchase(guest, totalPrice);
    }

    /**
     * 회원 8회권 구매.
     */
    public PassPurchase purchaseForMember(Long userId, long totalPrice) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime expiresAt = TimeBoundary.passExpiresAtLocal(now);

        PassPurchase purchase = passPurchaseRepository.save(new PassPurchase(userId, expiresAt, totalPrice));
        passLedgerRepository.save(new PassLedger(purchase, PassLedgerType.EARN, purchase.getTotalCredits()));

        return purchase;
    }

    private PassPurchase createPurchase(Guest guest, long totalPrice) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime expiresAt = TimeBoundary.passExpiresAtLocal(now);

        PassPurchase purchase = passPurchaseRepository.save(new PassPurchase(guest, expiresAt, totalPrice));
        passLedgerRepository.save(new PassLedger(purchase, PassLedgerType.EARN, purchase.getTotalCredits()));

        return purchase;
    }
}
