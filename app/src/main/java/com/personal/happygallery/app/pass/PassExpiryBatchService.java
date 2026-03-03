package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PassExpiryBatchService {

    private static final Logger log = LoggerFactory.getLogger(PassExpiryBatchService.class);

    private final PassPurchaseRepository passPurchaseRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public PassExpiryBatchService(PassPurchaseRepository passPurchaseRepository,
                                  PassLedgerRepository passLedgerRepository,
                                  NotificationService notificationService,
                                  Clock clock) {
        this.passPurchaseRepository = passPurchaseRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * 만료된 8회권의 잔여 크레딧을 소멸시킨다.
     *
     * <ol>
     *   <li>expires_at &lt; now AND remaining_credits &gt; 0 인 pass 조회</li>
     *   <li>각 pass에 대해 EXPIRE ledger 기록 → expire() 호출</li>
     * </ol>
     *
     * @return 처리된 건수
     */
    public int expireAll() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PassPurchase> expired = passPurchaseRepository
                .findByExpiresAtBeforeAndRemainingCreditsGreaterThan(now, 0);

        for (PassPurchase pass : expired) {
            int creditsToExpire = pass.getRemainingCredits();
            passLedgerRepository.save(new PassLedger(pass, PassLedgerType.EXPIRE, creditsToExpire));
            pass.expire();
            passPurchaseRepository.save(pass);
            log.info("Pass expired [passId={}] credits소멸={}", pass.getId(), creditsToExpire);
        }

        log.info("Pass expiry batch 완료: {}건 처리", expired.size());
        return expired.size();
    }

    /**
     * 만료 7일 전 알림 대상 조회.
     * now &lt;= expires_at &lt; now + 7일 AND remaining_credits &gt; 0
     */
    @Transactional(readOnly = true)
    public List<PassPurchase> findExpiringWithin7Days() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime in7Days = now.plusDays(7);
        return passPurchaseRepository
                .findByExpiresAtBetweenAndRemainingCreditsGreaterThan(now, in7Days, 0);
    }

    /**
     * 만료 7일 전 PASS_EXPIRY_SOON 알림 발송 배치.
     *
     * <p>JOIN FETCH guest 쿼리로 조회하여 detached 상태에서도 guest.id 접근이 안전하다.
     *
     * @return 발송 건수
     */
    public int sendExpiryNotifications() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime in7Days = now.plusDays(7);
        List<PassPurchase> expiring = passPurchaseRepository.findExpiringWithGuestBetween(now, in7Days);

        for (PassPurchase pass : expiring) {
            notificationService.notifyByGuestId(pass.getGuest().getId(), NotificationEventType.PASS_EXPIRY_SOON);
            log.info("8회권 만료 7일 전 알림 발송 [passId={}, guestId={}]", pass.getId(), pass.getGuest().getId());
        }

        log.info("8회권 만료 7일 전 알림 배치 완료: {}건", expiring.size());
        return expiring.size();
    }
}
