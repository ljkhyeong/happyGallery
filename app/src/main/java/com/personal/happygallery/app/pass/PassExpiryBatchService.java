package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PassExpiryBatchService {

    private static final Logger log = LoggerFactory.getLogger(PassExpiryBatchService.class);

    private final PassPurchaseRepository passPurchaseRepository;
    private final PassExpireProcessor passExpireProcessor;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public PassExpiryBatchService(PassPurchaseRepository passPurchaseRepository,
                                  PassExpireProcessor passExpireProcessor,
                                  NotificationLogRepository notificationLogRepository,
                                  NotificationService notificationService,
                                  Clock clock) {
        this.passPurchaseRepository = passPurchaseRepository;
        this.passExpireProcessor = passExpireProcessor;
        this.notificationLogRepository = notificationLogRepository;
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
    public BatchResult expireAll() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PassPurchase> expired = passPurchaseRepository
                .findByExpiresAtBeforeAndRemainingCreditsGreaterThan(now, 0);
        int processed = 0;
        Map<String, Integer> failureReasons = new LinkedHashMap<>();

        for (PassPurchase pass : expired) {
            try {
                if (passExpireProcessor.process(pass.getId())) {
                    processed++;
                }
            } catch (Exception e) {
                log.warn("8회권 만료 처리 실패 [passId={}]", pass.getId(), e);
                failureReasons.merge(e.getClass().getSimpleName(), 1, Integer::sum);
            }
        }

        return BatchResult.of(processed, failureReasons);
    }

    /**
     * 만료 7일 전 알림 대상 조회.
     * now &lt;= expires_at &lt; now + 7일 AND remaining_credits &gt; 0
     */
    @Transactional(readOnly = true)
    public List<PassPurchase> findExpiringWithin7Days() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime targetStart = now.plusDays(7).toLocalDate().atStartOfDay();
        LocalDateTime targetEnd = targetStart.plusDays(1);
        return passPurchaseRepository
                .findByExpiresAtBetweenAndRemainingCreditsGreaterThan(targetStart, targetEnd, 0);
    }

    /**
     * 만료 7일 전 PASS_EXPIRY_SOON 알림 발송 배치.
     *
     * <p>JOIN FETCH guest 쿼리로 조회하여 detached 상태에서도 guest.id 접근이 안전하다.
     *
     * @return 발송 건수
     */
    public BatchResult sendExpiryNotifications() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime targetStart = now.plusDays(7).toLocalDate().atStartOfDay();
        LocalDateTime targetEnd = targetStart.plusDays(1);
        LocalDateTime sentStart = now.toLocalDate().atStartOfDay();
        LocalDateTime sentEnd = sentStart.plusDays(1);
        List<PassPurchase> expiring = passPurchaseRepository.findExpiringWithGuestBetween(targetStart, targetEnd);
        int notified = 0;

        for (PassPurchase pass : expiring) {
            if (notificationLogRepository.existsByGuestIdAndEventTypeAndStatusAndSentAtBetween(
                    pass.getGuest().getId(),
                    NotificationEventType.PASS_EXPIRY_SOON,
                    "SUCCESS",
                    sentStart,
                    sentEnd)) {
                continue;
            }
            notificationService.notifyByGuestId(pass.getGuest().getId(), NotificationEventType.PASS_EXPIRY_SOON);
            log.info("8회권 만료 7일 전 알림 발송 [passId={}, guestId={}]", pass.getId(), pass.getGuest().getId());
            notified++;
        }

        return BatchResult.successOnly(notified);
    }
}
