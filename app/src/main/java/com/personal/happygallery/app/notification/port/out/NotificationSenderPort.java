package com.personal.happygallery.app.notification.port.out;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;

/**
 * 외부 알림 발송 포트.
 *
 * <p>application 서비스는 이 포트만 의존하며, 실제 채널 구현(카카오, SMS 등)은 adapter가 담당한다.
 * {@code @Order} 우선순위 순으로 주입되어 fallback 체인을 구성한다.
 */
public interface NotificationSenderPort {

    /** 이 sender가 담당하는 채널 */
    NotificationChannel channel();

    /**
     * 알림을 발송한다.
     *
     * @param phone         수신자 전화번호
     * @param recipientName 수신자 이름
     * @param eventType     발송 이벤트 유형
     * @return 발송 성공 여부
     */
    boolean send(String phone, String recipientName, NotificationEventType eventType);
}
