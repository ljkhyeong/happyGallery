package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;

/**
 * 주문/이행 정보 조회 + 부재 시 NotFoundException 던지기 패턴을 한 곳에 모은다.
 *
 * <p>여러 서비스가 동일한 메시지("주문" / "이행 정보")로 같은 라인을 반복하면서
 * 메시지를 바꿀 일이 생기면 누락되기 쉬웠다. 정적 유틸로 두면 호출 서비스의
 * DI 그래프는 그대로 두고 메시지/예외 결정만 한 군데서 관리된다.
 *
 * <p>{@code orElse(null)}, {@code orElseGet(...)} 등 다른 fallback 정책을 쓰는
 * 서비스(query, shipping의 prepareShipping)는 자기 컨텍스트가 명확하므로 직접 호출 유지.
 */
final class OrderLookups {

    private OrderLookups() {}

    static Order requireOrder(OrderReaderPort reader, Long orderId) {
        return reader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
    }

    static Fulfillment requireFulfillment(FulfillmentPort port, Long orderId) {
        return port.findByOrderId(orderId)
                .orElseThrow(NotFoundException.supplier("이행 정보"));
    }
}
