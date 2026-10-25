package com.personal.happygallery.domain.notification;

/** 알림 발송 채널. 실제 발송 채널은 KAKAO·SMS이고, SYSTEM은 발송 전 실패 기록용이다. */
public enum NotificationChannel {
    SYSTEM,
    KAKAO,
    SMS
}
