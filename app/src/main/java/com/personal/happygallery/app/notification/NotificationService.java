package com.personal.happygallery.app.notification;

import com.personal.happygallery.app.customer.port.out.GuestReaderPort;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.app.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 알림 발송 서비스.
 *
 * <p>주입된 {@link NotificationSenderPort} 목록을 {@code @Order} 우선순위 순으로 시도한다.
 * 한 채널이 성공하면 이후 채널은 시도하지 않는다 (fallback 전략).
 *
 * <p>채널 추가 시 {@link NotificationSenderPort} 구현체를 {@code @Order(n)}과 함께 등록하면
 * 이 서비스를 수정할 필요 없이 fallback 체인에 자동 포함된다.
 *
 * <p>알림 실패는 주문/예약 흐름을 중단시키지 않는다.
 * 호출자는 {@code notifyGuest()} 혹은 {@code notifyByGuestId()} 를 try-catch 없이 호출해도 된다.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<NotificationSenderPort> senders;
    private final NotificationLogStorePort notificationLogStore;
    private final GuestReaderPort guestReader;
    private final UserReaderPort userReader;
    private final Clock clock;

    public NotificationService(List<NotificationSenderPort> senders,
                               NotificationLogStorePort notificationLogStore,
                               GuestReaderPort guestReader,
                               UserReaderPort userReader,
                               Clock clock) {
        this.senders = senders;
        this.notificationLogStore = notificationLogStore;
        this.guestReader = guestReader;
        this.userReader = userReader;
        this.clock = clock;
    }

    /**
     * 게스트에게 알림을 발송한다. phone / name 을 이미 알고 있을 때 사용한다.
     *
     * <p>{@code @Order} 우선순위 순으로 시도한다. 한 채널이 성공하면 이후 채널은 시도하지 않는다.
     * 모든 채널 실패 시에도 예외를 던지지 않고 로그만 기록한다.
     *
     * <p>비동기로 실행되므로 호출자의 트랜잭션과 독립적이다.
     */
    @Async("notificationExecutor")
    public void notifyGuest(Long guestId, String phone, String name,
                            NotificationEventType eventType) {
        LocalDateTime sentAt = LocalDateTime.now(clock);
        for (NotificationSenderPort sender : senders) {
            try {
                boolean success = sender.send(phone, name, eventType);
                if (success) {
                    save(NotificationLog.success(guestId, null, sender.channel(), eventType, sentAt));
                    return;
                }
                save(NotificationLog.failed(guestId, null, sender.channel(), eventType, "발송 실패", sentAt));
            } catch (Exception e) {
                log.warn("[알림] {} 발송 예외 [guestId={} event={}]", sender.channel(), guestId, eventType, e);
                save(NotificationLog.failed(guestId, null, sender.channel(), eventType, e.getMessage(), sentAt));
            }
        }
        log.error("[알림] 모든 채널 실패 [guestId={} event={}]", guestId, eventType);
    }

    /**
     * guestId 만으로 게스트에게 알림을 발송한다.
     * 게스트가 존재하지 않으면 경고 로그를 남기고 무시한다.
     *
     * <p>비동기로 실행되므로 호출자의 트랜잭션과 독립적이다.
     */
    @Async("notificationExecutor")
    public void notifyByGuestId(Long guestId, NotificationEventType eventType) {
        if (guestId == null) {
            return;
        }
        guestReader.findById(guestId).ifPresentOrElse(
                guest -> notifyGuest(guest.getId(), guest.getPhone(), guest.getName(), eventType),
                () -> log.warn("[알림] 게스트 미존재 [guestId={}]", guestId)
        );
    }

    /**
     * userId 만으로 회원에게 알림을 발송한다.
     * 회원이 존재하지 않으면 경고 로그를 남기고 무시한다.
     */
    @Async("notificationExecutor")
    public void notifyByUserId(Long userId, NotificationEventType eventType) {
        if (userId == null) {
            return;
        }
        userReader.findById(userId).ifPresentOrElse(
                user -> notifyUser(userId, user.getPhone(), user.getName(), eventType),
                () -> log.warn("[알림] 회원 미존재 [userId={}]", userId)
        );
    }

    /**
     * 회원에게 알림을 발송한다. phone / name 을 이미 알고 있을 때 사용한다.
     */
    @Async("notificationExecutor")
    public void notifyUser(Long userId, String phone, String name,
                           NotificationEventType eventType) {
        LocalDateTime sentAt = LocalDateTime.now(clock);
        for (NotificationSenderPort sender : senders) {
            try {
                boolean success = sender.send(phone, name, eventType);
                if (success) {
                    save(NotificationLog.success(null, userId, sender.channel(), eventType, sentAt));
                    return;
                }
                save(NotificationLog.failed(null, userId, sender.channel(), eventType, "발송 실패", sentAt));
            } catch (Exception e) {
                log.warn("[알림] {} 발송 예외 [userId={} event={}]", sender.channel(), userId, eventType, e);
                save(NotificationLog.failed(null, userId, sender.channel(), eventType, e.getMessage(), sentAt));
            }
        }
        log.error("[알림] 모든 채널 실패 [userId={} event={}]", userId, eventType);
    }

    private void save(NotificationLog entry) {
        try {
            notificationLogStore.save(entry);
        } catch (Exception e) {
            log.error("[알림] 로그 저장 실패", e);
        }
    }
}
