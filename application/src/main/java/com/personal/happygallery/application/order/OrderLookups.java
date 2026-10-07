package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 주문/이행 정보 조회 + 부재 시 NotFoundException 던지기 패턴을 한 곳에 모은다.
 *
 * <p>여러 서비스가 동일한 메시지("주문" / "이행 정보")로 같은 라인을 반복하면서
 * 메시지를 바꿀 일이 생기면 누락되기 쉬웠다. 정적 유틸로 두면 호출 서비스의
 * DI 그래프는 그대로 두고 메시지/예외 결정만 한 군데서 관리된다.
 *
 * <p>회원·비회원 소유권 확인도 여기서 한다. 다른 소유자의 주문은 존재 여부를 드러내지 않도록
 * 없는 주문과 같은 NotFoundException("주문")으로 처리한다. 비회원 토큰은 주문을 읽기 전에 검증한다.
 */
final class OrderLookups {

    private OrderLookups() {}

    static Order requireOrder(OrderReaderPort reader, Long orderId) {
        return reader.findById(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
    }

    static Order requireMemberOrder(OrderReaderPort reader, Long orderId, Long userId) {
        return requireOwned(reader.findById(orderId), ownedBy(userId));
    }

    static Order requireMemberOrderForUpdate(OrderReaderPort reader, Long orderId, Long userId) {
        return requireOwned(reader.findByIdForUpdate(orderId), ownedBy(userId));
    }

    static Order requireGuestOrder(
            OrderReaderPort reader, GuestTokenService tokens, Long orderId, String rawToken) {
        Predicate<Order> owner = accessibleBy(tokens.resolveTokenHash(rawToken));
        return requireOwned(reader.findById(orderId), owner);
    }

    static Order requireGuestOrderForUpdate(
            OrderReaderPort reader, GuestTokenService tokens, Long orderId, String rawToken) {
        Predicate<Order> owner = accessibleBy(tokens.resolveTokenHash(rawToken));
        return requireOwned(reader.findByIdForUpdate(orderId), owner);
    }

    static Fulfillment requireFulfillment(FulfillmentPort port, Long orderId) {
        return port.findByOrderId(orderId)
                .orElseThrow(NotFoundException.supplier("이행 정보"));
    }

    private static Predicate<Order> ownedBy(Long userId) {
        return order -> Objects.equals(order.getUserId(), userId);
    }

    private static Predicate<Order> accessibleBy(String tokenHash) {
        return order -> Objects.equals(order.getAccessToken(), tokenHash);
    }

    private static Order requireOwned(Optional<Order> order, Predicate<Order> owner) {
        return order.filter(owner).orElseThrow(NotFoundException.supplier("주문"));
    }
}
