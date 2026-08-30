package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationOutbox;

/** 알림 outbox 멱등키를 DB 유일 제약으로 원자적으로 선점한다. */
public interface NotificationOutboxInsertPort {

    /**
     * @return 새 행을 저장했으면 {@code true}, 같은 멱등키가 이미 존재하면 {@code false}
     */
    boolean insertIfAbsent(NotificationOutbox outbox);
}
