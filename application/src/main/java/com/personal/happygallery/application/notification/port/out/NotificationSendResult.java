package com.personal.happygallery.application.notification.port.out;

/** 외부 알림 채널 호출 결과와 재시도 가능성을 함께 전달한다. */
public enum NotificationSendResult {
    ACCEPTED,
    SUCCESS,
    PERMANENT_FAILURE,
    TRANSIENT_FAILURE,
    DELIVERY_UNKNOWN;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
