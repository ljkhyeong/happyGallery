package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;

/** 재발송 여부를 판단할 수 있도록 접수 실패와 결과 미확인을 구분한다. */
interface EmailVerificationTransport {
    NotificationSendResult sendResult(String email, String verificationCode);
}
