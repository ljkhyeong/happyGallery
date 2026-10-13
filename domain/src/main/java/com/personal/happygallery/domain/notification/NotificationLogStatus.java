package com.personal.happygallery.domain.notification;

/** 알림 발송 이력 상태. REQUESTED는 제공자 접수 후 최종 결과 조회를 기다리는 상태다. */
public enum NotificationLogStatus {
    SUCCESS,
    FAILED,
    REQUESTED
}
