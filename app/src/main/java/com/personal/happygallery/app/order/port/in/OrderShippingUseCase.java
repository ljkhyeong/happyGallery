package com.personal.happygallery.app.order.port.in;

import com.personal.happygallery.app.order.OrderShippingService.ShippingResult;

/**
 * 배송 이행 관리 유스케이스.
 *
 * <p>배송 준비, 배송 출발, 배송 완료를 지원한다.
 */
public interface OrderShippingUseCase {

    ShippingResult prepareShipping(Long orderId, Long adminId);

    ShippingResult markShipped(Long orderId, Long adminId);

    ShippingResult markDelivered(Long orderId, Long adminId);
}
