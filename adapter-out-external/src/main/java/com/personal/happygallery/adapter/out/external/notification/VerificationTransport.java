package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;

/**
 * 휴대폰·이메일 인증 코드를 외부 채널로 한 건 발송한다.
 *
 * <p>재발송 여부를 판단할 수 있도록 접수 실패와 결과 미확인을 구분한다.
 */
interface VerificationTransport {
    NotificationSendResult sendResult(String target, String verificationCode);
}
