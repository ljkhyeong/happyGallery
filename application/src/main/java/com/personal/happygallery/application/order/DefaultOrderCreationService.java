package com.personal.happygallery.application.order;

import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.product.Product;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

/**
 * 회원 장바구니 주문 생성 조합 서비스.
 */
@Service
@Transactional
public class DefaultOrderCreationService implements OrderCreationService {

    private final ProductReaderPort productReader;
    private final OrderService orderService;

    public DefaultOrderCreationService(ProductReaderPort productReader,
                                       OrderService orderService) {
        this.productReader = productReader;
        this.orderService = orderService;
    }

    /**
     * 회원 주문 생성 — 세션 userId 기반.
     */
    public Order createMemberOrder(Long userId, List<OrderItemInput> items) {
        List<OrderService.OrderItemRequest> orderItems = resolveItemPrices(items);
        return orderService.createMemberOrder(userId, orderItems);
    }

    private List<OrderService.OrderItemRequest> resolveItemPrices(List<OrderItemInput> items) {
        Map<Long, Product> productsById = productReader.findAllById(items.stream()
                        .map(OrderItemInput::productId)
                        .distinct()
                        .toList())
                .stream()
                .collect(toMap(Product::getId, Function.identity()));

        return items.stream()
                .map(item -> toOrderItem(item, productsById))
                .toList();
    }

    private OrderService.OrderItemRequest toOrderItem(OrderItemInput item, Map<Long, Product> productsById) {
        Product product = productsById.get(item.productId());
        if (product == null) {
            throw new NotFoundException("상품");
        }
        return new OrderService.OrderItemRequest(item.productId(), item.qty(), product.getPrice());
    }
}
