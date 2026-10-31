package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.time.LocalDateTime;

/** 주문 이력(결정 기록) 조회 결과 */
public record OrderHistoryView(
        Long id,
        OrderApprovalDecision decision,
        Long decidedByAdminId,
        String reason,
        LocalDateTime decidedAt
) {
    public static OrderHistoryView from(OrderApprovalHistory h) {
        return new OrderHistoryView(h.getId(), h.getDecision(), h.getDecidedByAdminId(), h.getReason(), h.getDecidedAt());
    }
}
