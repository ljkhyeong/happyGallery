package com.personal.happygallery.application.pass;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.notification.NotificationOutboxService;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DefaultPassExpiryBatchService implements PassExpiryBatchUseCase {

    private final PassPurchaseReaderPort passPurchaseReader;
    private final PassExpireProcessor passExpireProcessor;
    private final NotificationOutboxService notificationOutboxService;
    private final Clock clock;

    public DefaultPassExpiryBatchService(PassPurchaseReaderPort passPurchaseReader,
                                          PassExpireProcessor passExpireProcessor,
                                          NotificationOutboxService notificationOutboxService,
                                          Clock clock) {
        this.passPurchaseReader = passPurchaseReader;
        this.passExpireProcessor = passExpireProcessor;
        this.notificationOutboxService = notificationOutboxService;
        this.clock = clock;
    }

    /**
     * 만료된 8회권의 잔여 크레딧을 소멸시킨다.
     *
     * <ol>
     *   <li>expires_at &lt;= now AND remaining_credits &gt; 0 인 pass 조회</li>
     *   <li>각 pass에 대해 EXPIRE ledger 기록 → expire() 호출</li>
     * </ol>
     *
     * @return 처리된 건수
     */
    private static final int PAGE_SIZE = 100;

    @Override
    public BatchResult expireAll() {
        LocalDateTime now = LocalDateTime.now(clock);

        return BatchExecutor.executeByIdCursor(
                afterId -> passPurchaseReader.findExpiredWithRemainingCreditsAfterId(
                        now, 0, afterId, PageRequest.ofSize(PAGE_SIZE)),
                PassPurchase::getId,
                pass -> passExpireProcessor.process(pass.getId()),
                "8회권 만료");
    }

    /**
     * 만료 7일 전 PASS_EXPIRY_SOON 알림 발송 배치.
     *
     * @return 발송 건수
     */
    @Override
    public BatchResult sendExpiryNotifications() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PassPurchase> expiring = findPassesExpiringInSevenDays(now);

        return BatchExecutor.execute(expiring,
                PassPurchase::getId,
                this::requestExpiryNotification,
                "8회권 만료 알림");
    }

    private List<PassPurchase> findPassesExpiringInSevenDays(LocalDateTime now) {
        LocalDateTime targetStart = now.plusDays(7).toLocalDate().atStartOfDay();
        LocalDateTime targetEnd = targetStart.plusDays(1);
        return passPurchaseReader
                .findByExpiresAtBetweenAndRemainingCreditsGreaterThan(targetStart, targetEnd, 0);
    }

    private boolean requestExpiryNotification(PassPurchase pass) {
        NotificationRequestedEvent event = NotificationRequestedEvent.forUser(
                pass.getUserId(),
                NotificationEventType.PASS_EXPIRY_SOON,
                "PASS_PURCHASE",
                pass.getId());
        return notificationOutboxService.enqueue(event);
    }
}
