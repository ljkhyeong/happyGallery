package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Order;

/**
 * 주문 승인/거절 유스케이스.
 *
 * <p>관리자 화면에서 주문을 승인하거나 거절할 때 호출된다.
 */
public interface OrderApprovalUseCase {

    record RejectResult(Order order, Refund refund) {}

    Order approve(Long orderId);

    Order approve(Long orderId, Long adminId);

    RejectResult reject(Long orderId);

    RejectResult reject(Long orderId, Long adminId);
}
