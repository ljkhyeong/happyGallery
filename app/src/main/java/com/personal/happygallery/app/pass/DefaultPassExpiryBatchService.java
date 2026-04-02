package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.batch.BatchExecutor;
import com.personal.happygallery.app.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.app.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPassExpiryBatchService implements PassExpiryBatchUseCase {

    private final PassPurchaseReaderPort passPurchaseReader;
    private final PassExpireProcessor passExpireProcessor;
    private final NotificationLogReaderPort notificationLogReader;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DefaultPassExpiryBatchService(PassPurchaseReaderPort passPurchaseReader,
                                  PassExpireProcessor passExpireProcessor,
                                  NotificationLogReaderPort notificationLogReader,
                                  ApplicationEventPublisher eventPublisher,
                                  Clock clock) {
        this.passPurchaseReader = passPurchaseReader;
        this.passExpireProcessor = passExpireProcessor;
        this.notificationLogReader = notificationLogReader;
        this.eventPublisher = eventPublisher;
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
    private static final int PAGE_SIZE = 100;

    public BatchResult expireAll() {
        LocalDateTime now = LocalDateTime.now(clock);

        return BatchExecutor.executePaginated(
                () -> passPurchaseReader
                        .findByExpiresAtBeforeAndRemainingCreditsGreaterThan(now, 0, PageRequest.ofSize(PAGE_SIZE)),
                PassPurchase::getId,
                pass -> passExpireProcessor.process(pass.getId()),
                "8회권 만료");
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
        return passPurchaseReader
                .findByExpiresAtBetweenAndRemainingCreditsGreaterThan(targetStart, targetEnd, 0);
    }

    /**
     * 만료 7일 전 PASS_EXPIRY_SOON 알림 발송 배치.
     *
     * <p>중복 발송 체크 로직이 포함되어 범용 BatchExecutor를 사용하지 않는다.
     *
     * @return 발송 건수
     */
    public BatchResult sendExpiryNotifications() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime targetStart = now.plusDays(7).toLocalDate().atStartOfDay();
        LocalDateTime targetEnd = targetStart.plusDays(1);
        LocalDateTime sentStart = now.toLocalDate().atStartOfDay();
        LocalDateTime sentEnd = sentStart.plusDays(1);
        List<PassPurchase> expiring = passPurchaseReader
                .findByExpiresAtBetweenAndRemainingCreditsGreaterThan(targetStart, targetEnd, 0);

        return BatchExecutor.execute(expiring,
                PassPurchase::getId,
                pass -> {
                    if (notificationLogReader.existsSentUserNotification(
                            pass.getUserId(),
                            NotificationEventType.PASS_EXPIRY_SOON,
                            sentStart,
                            sentEnd)) {
                        return false;
                    }
                    eventPublisher.publishEvent(NotificationRequestedEvent.forUser(pass.getUserId(), NotificationEventType.PASS_EXPIRY_SOON));
                    return true;
                },
                "8회권 만료 알림");
    }
}
