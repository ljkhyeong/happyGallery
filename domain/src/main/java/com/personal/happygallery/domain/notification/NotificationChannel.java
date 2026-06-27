package com.personal.happygallery.domain.notification;

/** 알림 발송 채널. SYSTEM은 실제 발송 채널이 아니라 발송 전 실패 기록용이다. */
public enum NotificationChannel {
    SYSTEM,
    KAKAO,
    SMS,
    EMAIL,
    PUSH
}
