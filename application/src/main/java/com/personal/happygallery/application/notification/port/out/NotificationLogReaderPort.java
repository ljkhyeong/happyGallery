package com.personal.happygallery.application.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 발송 기록 조회 포트 — 배치 알림의 최근 성공 여부 확인에 사용한다.
 */
public interface NotificationLogReaderPort {

    List<Long> findSentGuestIds(List<Long> guestIds, NotificationEventType eventType,
                                LocalDateTime sentStart, LocalDateTime sentEnd);

    List<Long> findSentUserIds(List<Long> userIds, NotificationEventType eventType,
                               LocalDateTime sentStart, LocalDateTime sentEnd);

}
