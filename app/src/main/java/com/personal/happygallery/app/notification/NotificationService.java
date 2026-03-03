package com.personal.happygallery.app.notification;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import com.personal.happygallery.infra.notification.NotificationSender;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 알림 발송 서비스.
 *
 * <p>채널 우선순위: 카카오 → 실패 시 SMS (fallback).
 * 각 발송 결과는 {@code notification_log}에 기록된다.
 *
 * <p>알림 실패는 주문/예약 흐름을 중단시키지 않는다.
 * 호출자는 {@code notifyGuest()} 혹은 {@code notifyByGuestId()} 를 try-catch 없이 호출해도 된다.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** fallback 순서 고정 */
    private static final List<NotificationChannel> FALLBACK_ORDER =
            List.of(NotificationChannel.KAKAO, NotificationChannel.SMS);

    private final Map<NotificationChannel, NotificationSender> senders;
    private final NotificationLogRepository notificationLogRepository;
    private final GuestRepository guestRepository;

    public NotificationService(List<NotificationSender> senders,
                               NotificationLogRepository notificationLogRepository,
                               GuestRepository guestRepository) {
        this.senders = senders.stream()
                .collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
        this.notificationLogRepository = notificationLogRepository;
        this.guestRepository = guestRepository;
    }

    /**
     * 게스트에게 알림을 발송한다. phone / name 을 이미 알고 있을 때 사용한다.
     *
     * <p>카카오 → SMS 순으로 시도한다. 한 채널이 성공하면 이후 채널은 시도하지 않는다.
     * 모든 채널 실패 시에도 예외를 던지지 않고 로그만 기록한다.
     */
    public void notifyGuest(Long guestId, String phone, String name,
                            NotificationEventType eventType) {
        for (NotificationChannel channel : FALLBACK_ORDER) {
            NotificationSender sender = senders.get(channel);
            if (sender == null) {
                continue;
            }
            try {
                boolean success = sender.send(phone, name, eventType);
                if (success) {
                    save(NotificationLog.success(guestId, null, channel, eventType));
                    return;
                }
                save(NotificationLog.failed(guestId, null, channel, eventType, "발송 실패"));
            } catch (Exception e) {
                log.warn("[알림] {} 발송 예외 [guestId={} event={}]", channel, guestId, eventType, e);
                save(NotificationLog.failed(guestId, null, channel, eventType, e.getMessage()));
            }
        }
        log.error("[알림] 모든 채널 실패 [guestId={} event={}]", guestId, eventType);
    }

    /**
     * guestId 만으로 게스트에게 알림을 발송한다.
     * 게스트가 존재하지 않으면 경고 로그를 남기고 무시한다.
     */
    public void notifyByGuestId(Long guestId, NotificationEventType eventType) {
        if (guestId == null) {
            return;
        }
        guestRepository.findById(guestId).ifPresentOrElse(
                guest -> notifyGuest(guest.getId(), guest.getPhone(), guest.getName(), eventType),
                () -> log.warn("[알림] 게스트 미존재 [guestId={}]", guestId)
        );
    }

    private void save(NotificationLog entry) {
        try {
            notificationLogRepository.save(entry);
        } catch (Exception e) {
            log.error("[알림] 로그 저장 실패", e);
        }
    }
}
