package com.personal.happygallery.app.order.port.in;

import com.personal.happygallery.app.order.OrderCreationService.OrderItemInput;
import com.personal.happygallery.app.order.OrderService.OrderCreationResult;
import com.personal.happygallery.domain.order.Order;
import java.util.List;

/**
 * 주문 생성 유스케이스.
 *
 * <p>휴대폰 인증 기반(비회원) / 회원 두 경로를 지원한다.
 */
public interface OrderCreationUseCase {

    OrderCreationResult createOrderByPhone(String phone, String verificationCode,
                                           String name, List<OrderItemInput> items);

    Order createMemberOrder(Long userId, List<OrderItemInput> items);
}
