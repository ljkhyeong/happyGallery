package com.personal.happygallery.application.notification.port.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 회원 알림 원본에서 수신자 본인 소유의 주문·예약·빈자리·재입고·상품 문의 정보만 조회한다.
 * 클레임·환불 알림은 원래 주문·예약으로 이어 준다.
 */
public interface NotificationContextPort {
    record Context(Long notificationId, String name, int itemCount, LocalDateTime scheduledAt,
                   Long orderId, Long bookingId, Long productId, Long productVariantId,
                   Long classId, Long slotId) {}

    List<Context> findContexts(Collection<Long> notificationIds, Long userId);
}
