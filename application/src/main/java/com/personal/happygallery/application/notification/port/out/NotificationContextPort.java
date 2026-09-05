package com.personal.happygallery.application.notification.port.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** 알림 수신자와 원본 소유자가 일치하는 주문·예약·재입고 정보만 조회한다. */
public interface NotificationContextPort {
    record Context(Long notificationId, String name, int itemCount, LocalDateTime scheduledAt) {}

    List<Context> findContexts(Collection<Long> notificationIds, Long userId, Long guestId);
}
