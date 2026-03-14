package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderApprovalHistory;
import java.time.LocalDateTime;

/** 주문 이력(결정 기록) 응답 */
public record OrderHistoryResponse(
        Long id,
        OrderApprovalDecision decision,
        Long decidedByAdminId,
        String reason,
        LocalDateTime decidedAt
) {
    public static OrderHistoryResponse from(OrderApprovalHistory h) {
        return new OrderHistoryResponse(h.getId(), h.getDecision(), h.getDecidedByAdminId(), h.getReason(), h.getDecidedAt());
    }
}
