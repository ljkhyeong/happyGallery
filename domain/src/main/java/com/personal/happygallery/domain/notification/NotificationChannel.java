package com.personal.happygallery.domain.notification;

/** 알림 발송 채널. notification_log.channel 컬럼 값으로 사용된다. */
public enum NotificationChannel {
    KAKAO,
    SMS,
    EMAIL,
    PUSH
}
