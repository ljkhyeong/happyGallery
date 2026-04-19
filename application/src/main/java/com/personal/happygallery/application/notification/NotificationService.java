package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        this.senders = List.copyOf(senders);
        this.notificationLogStore = notificationLogStore;
        this.guestReader = guestReader;
        this.userReader = userReader;
        this.clock = clock;
    }

    // -- 이벤트 리스너용 package-private 메서드 --

    void sendToGuest(Long guestId, String phone, String name,
                     NotificationEventType eventType) {
        sendNotification(guestId, null, phone, name, eventType);
    }

    void sendByGuestId(Long guestId, NotificationEventType eventType) {
        if (guestId == null) {
            return;
        }
        guestReader.findById(guestId).ifPresentOrElse(
                guest -> sendToGuest(guest.getId(), guest.getPhone(), guest.getName(), eventType),
                () -> log.warn("[알림] 게스트 미존재 [guestId={}]", guestId)
        );
    }

    void sendByUserId(Long userId, NotificationEventType eventType) {
        if (userId == null) {
            return;
        }
        userReader.findById(userId).ifPresentOrElse(
                user -> sendToUser(userId, user.getPhone(), user.getName(), eventType),
                () -> log.warn("[알림] 회원 미존재 [userId={}]", userId)
        );
    }

    void sendToUser(Long userId, String phone, String name,
                    NotificationEventType eventType) {
        sendNotification(null, userId, phone, name, eventType);
    }

    private void sendNotification(Long guestId, Long userId, String phone, String name,
                                  NotificationEventType eventType) {
        LocalDateTime sentAt = LocalDateTime.now(clock);
        Long recipientId = guestId != null ? guestId : userId;
        String recipientLabel = guestId != null ? "guestId" : "userId";

        for (NotificationSenderPort sender : senders) {
            try {
                boolean success = sender.send(phone, name, eventType);
                if (success) {
                    save(NotificationLog.success(guestId, userId, sender.channel(), eventType, sentAt));
                    return;
                }
                save(NotificationLog.failed(guestId, userId, sender.channel(), eventType, "발송 실패", sentAt));
            } catch (Exception e) {
                log.warn("[알림] {} 발송 예외 [{}={} event={}]", sender.channel(), recipientLabel, recipientId, eventType, e);
                save(NotificationLog.failed(guestId, userId, sender.channel(), eventType, e.getMessage(), sentAt));
            }
        }
        log.error("[알림] 모든 채널 실패 [{}={} event={}]", recipientLabel, recipientId, eventType);
    }

    private void save(NotificationLog entry) {
        try {
            notificationLogStore.save(entry);
        } catch (Exception e) {
            log.error("[알림] 로그 저장 실패", e);
        }
    }
}
