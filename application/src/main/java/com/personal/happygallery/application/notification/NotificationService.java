package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.application.notification.port.out.NotificationSendOutcome;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.application.notification.port.out.TrackedNotificationSenderPort;
import com.personal.happygallery.application.monitoring.AppMetrics;
import com.personal.happygallery.domain.notification.NotificationChannel;
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
 * <p>외부 채널 실패는 outbox 재시도 대상으로 반환하며 주문/예약 트랜잭션과 분리한다.
 * 외부 발송 성공 뒤 감사 로그만 실패한 경우에는 재발송하지 않고 별도 운영 경고로 남긴다.
 */
@Service
public class NotificationService {

    private static final String RECIPIENT_NOT_FOUND = "RECIPIENT_NOT_FOUND";
    private static final String DELIVERY_EXCEPTION = "DELIVERY_EXCEPTION";

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<NotificationSenderPort> senders;
    private final NotificationLogStorePort notificationLogStore;
    private final GuestReaderPort guestReader;
    private final UserReaderPort userReader;
    private final GuestPersonalDataProtector guestPersonalDataProtector;
    private final AppMetrics metrics;
    private final Clock clock;

    public NotificationService(List<NotificationSenderPort> senders,
                               NotificationLogStorePort notificationLogStore,
                               GuestReaderPort guestReader,
                               UserReaderPort userReader,
                               GuestPersonalDataProtector guestPersonalDataProtector,
                               AppMetrics metrics,
                               Clock clock) {
        this.senders = List.copyOf(senders);
        this.notificationLogStore = notificationLogStore;
        this.guestReader = guestReader;
        this.userReader = userReader;
        this.guestPersonalDataProtector = guestPersonalDataProtector;
        this.metrics = metrics;
        this.clock = clock;
    }

    // -- outbox dispatcher 용 package-private 메서드 --

    NotificationSendResult sendToGuest(Long guestId, String idempotencyKey, String phone, String name,
                                       NotificationEventType eventType) {
        return sendNotification(guestId, null, idempotencyKey, phone, name, eventType, null).result();
    }

    NotificationSendResult sendByGuestId(Long guestId,
                                         NotificationEventType eventType,
                                         String idempotencyKey) {
        if (guestId == null) {
            return NotificationSendResult.SUCCESS;
        }
        return guestReader.findById(guestId)
                .map(guest -> sendToGuest(
                        guest.getId(),
                        idempotencyKey,
                        guestPersonalDataProtector.decryptPhone(guest),
                        guestPersonalDataProtector.decryptName(guest),
                        eventType))
                .orElseGet(() -> {
                    if (!logRecipientNotFound(guestId, null, eventType)) {
                        throw NotificationAuditPersistenceException.afterCompletedDelivery();
                    }
                    return NotificationSendResult.PERMANENT_FAILURE;
                });
    }

    NotificationSendResult sendByUserId(Long userId,
                                        NotificationEventType eventType,
                                        String idempotencyKey) {
        if (userId == null) {
            return NotificationSendResult.SUCCESS;
        }
        return userReader.findById(userId)
                .map(user -> sendToUser(userId, idempotencyKey, user.getPhone(), user.getName(), eventType))
                .orElseGet(() -> {
                    if (!logRecipientNotFound(null, userId, eventType)) {
                        throw NotificationAuditPersistenceException.afterCompletedDelivery();
                    }
                    return NotificationSendResult.PERMANENT_FAILURE;
                });
    }

    NotificationSendResult sendToUser(Long userId, String idempotencyKey, String phone, String name,
                                      NotificationEventType eventType) {
        return sendNotification(null, userId, idempotencyKey, phone, name, eventType, null).result();
    }

    NotificationDeliveryAttempt sendByGuestIdWithOutcome(
            Long guestId,
            NotificationEventType eventType,
            String idempotencyKey,
            NotificationChannel startAfterChannel) {
        if (guestId == null) {
            return NotificationDeliveryAttempt.immediate(NotificationSendResult.SUCCESS);
        }
        return guestReader.findById(guestId)
                .map(guest -> sendNotification(
                        guest.getId(),
                        null,
                        idempotencyKey,
                        guestPersonalDataProtector.decryptPhone(guest),
                        guestPersonalDataProtector.decryptName(guest),
                        eventType,
                        startAfterChannel))
                .orElseGet(() -> recipientNotFoundAttempt(guestId, null, eventType));
    }

    NotificationDeliveryAttempt sendByUserIdWithOutcome(
            Long userId,
            NotificationEventType eventType,
            String idempotencyKey,
            NotificationChannel startAfterChannel) {
        if (userId == null) {
            return NotificationDeliveryAttempt.immediate(NotificationSendResult.SUCCESS);
        }
        return userReader.findById(userId)
                .map(user -> sendNotification(
                        null,
                        userId,
                        idempotencyKey,
                        user.getPhone(),
                        user.getName(),
                        eventType,
                        startAfterChannel))
                .orElseGet(() -> recipientNotFoundAttempt(null, userId, eventType));
    }

    private NotificationDeliveryAttempt sendNotification(Long guestId,
                                                         Long userId,
                                                         String idempotencyKey,
                                                         String phone,
                                                         String name,
                                                         NotificationEventType eventType,
                                                         NotificationChannel startAfterChannel) {
        LocalDateTime sentAt = LocalDateTime.now(clock);
        Long recipientId = guestId != null ? guestId : userId;
        String recipientLabel = guestId != null ? "guestId" : "userId";
        boolean auditPersisted = true;
        boolean transientFailure = false;

        boolean startChannelPassed = startAfterChannel == null;
        for (NotificationSenderPort sender : senders) {
            if (!startChannelPassed) {
                startChannelPassed = sender.channel() == startAfterChannel;
                continue;
            }
            NotificationSendOutcome outcome;
            try {
                outcome = sender instanceof TrackedNotificationSenderPort tracked
                        ? tracked.sendTracked(idempotencyKey, phone, name, eventType)
                        : NotificationSendOutcome.immediate(
                                sender.send(idempotencyKey, phone, name, eventType));
            } catch (Exception e) {
                log.warn("[알림] {} 발송 예외 [{}={} event={} type={}]",
                        sender.channel(), recipientLabel, recipientId, eventType, e.getClass().getSimpleName());
                auditPersisted &= save(NotificationLog.failed(
                        guestId, userId, sender.channel(), eventType, DELIVERY_EXCEPTION, sentAt));
                log.error("[알림] 발송 결과 확인 필요 [{}={} event={}]",
                        recipientLabel, recipientId, eventType);
                if (!auditPersisted) {
                    throw NotificationAuditPersistenceException.afterUnknownDelivery();
                }
                return NotificationDeliveryAttempt.immediate(NotificationSendResult.DELIVERY_UNKNOWN);
            }

            NotificationSendResult result = outcome.result();
            if (result == NotificationSendResult.ACCEPTED) {
                auditPersisted &= save(NotificationLog.requested(
                        guestId,
                        userId,
                        sender.channel(),
                        eventType,
                        outcome.providerRequestId(),
                        outcome.providerRecipientSeq(),
                        sentAt));
                return NotificationDeliveryAttempt.from(sender.channel(), outcome);
            }
            if (result.isSuccess()) {
                auditPersisted &= save(
                        NotificationLog.success(guestId, userId, sender.channel(), eventType, sentAt));
                if (!auditPersisted) {
                    throw NotificationAuditPersistenceException.afterCompletedDelivery();
                }
                return NotificationDeliveryAttempt.from(sender.channel(), outcome);
            }
            auditPersisted &= save(NotificationLog.failed(
                    guestId, userId, sender.channel(), eventType, result.name(), sentAt));
            if (result == NotificationSendResult.DELIVERY_UNKNOWN) {
                log.error("[알림] 발송 결과 확인 필요 [{}={} event={}]",
                        recipientLabel, recipientId, eventType);
                if (!auditPersisted) {
                    throw NotificationAuditPersistenceException.afterUnknownDelivery();
                }
                return NotificationDeliveryAttempt.from(sender.channel(), outcome);
            }
            transientFailure |= result == NotificationSendResult.TRANSIENT_FAILURE;
        }
        log.error("[알림] 모든 채널 실패 [{}={} event={}]", recipientLabel, recipientId, eventType);
        if (!auditPersisted) {
            throw NotificationAuditPersistenceException.beforeCompletedDelivery();
        }
        return NotificationDeliveryAttempt.immediate(transientFailure
                ? NotificationSendResult.TRANSIENT_FAILURE
                : NotificationSendResult.PERMANENT_FAILURE);
    }

    private NotificationDeliveryAttempt recipientNotFoundAttempt(
            Long guestId, Long userId, NotificationEventType eventType) {
        if (!logRecipientNotFound(guestId, userId, eventType)) {
            throw NotificationAuditPersistenceException.afterCompletedDelivery();
        }
        return NotificationDeliveryAttempt.immediate(NotificationSendResult.PERMANENT_FAILURE);
    }

    private boolean logRecipientNotFound(Long guestId, Long userId, NotificationEventType eventType) {
        Long recipientId = guestId != null ? guestId : userId;
        String recipientLabel = guestId != null ? "guestId" : "userId";
        log.warn("[알림] 수신자 미존재 [{}={} event={}]", recipientLabel, recipientId, eventType);
        return save(NotificationLog.failed(
                guestId,
                userId,
                NotificationChannel.SYSTEM,
                eventType,
                RECIPIENT_NOT_FOUND,
                LocalDateTime.now(clock)
        ));
    }

    private boolean save(NotificationLog entry) {
        try {
            notificationLogStore.save(entry);
            return true;
        } catch (Exception e) {
            metrics.incrementNotificationLogPersistenceFailure();
            log.error("[알림] 로그 저장 실패 [channel={} event={} type={}]",
                    entry.getChannel(), entry.getEventType(), e.getClass().getSimpleName());
            return false;
        }
    }
}

final class NotificationAuditPersistenceException extends RuntimeException {

    private final NotificationSendResult deliveryResult;

    private NotificationAuditPersistenceException(NotificationSendResult deliveryResult) {
        super("알림 결과 감사 이력을 저장하지 못했습니다.");
        this.deliveryResult = deliveryResult;
    }

    static NotificationAuditPersistenceException afterCompletedDelivery() {
        return new NotificationAuditPersistenceException(NotificationSendResult.SUCCESS);
    }

    static NotificationAuditPersistenceException beforeCompletedDelivery() {
        return new NotificationAuditPersistenceException(NotificationSendResult.TRANSIENT_FAILURE);
    }

    static NotificationAuditPersistenceException afterUnknownDelivery() {
        return new NotificationAuditPersistenceException(NotificationSendResult.DELIVERY_UNKNOWN);
    }

    NotificationSendResult deliveryResult() {
        return deliveryResult;
    }
}
