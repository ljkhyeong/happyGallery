package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderDelayDecision;

/** 회원과 비회원 고객의 주문 취소 및 제작 지연 응답 유스케이스. */
public interface OrderCustomerActionUseCase {

    ActionResult cancelGuestOrder(Long orderId, String accessToken);

    ActionResult cancelMemberOrder(Long orderId, Long userId);

    ActionResult respondToGuestDelay(
            Long orderId, String accessToken, OrderDelayDecision decision);

    ActionResult respondToMemberDelay(
            Long orderId, Long userId, OrderDelayDecision decision);

    record ActionResult(Order order, Refund refund) {}
}
