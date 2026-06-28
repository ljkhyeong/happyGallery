package com.personal.happygallery.application.order;

import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.product.Product;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return items.stream()
                .map(item -> {
                    Product product = productReader.findById(item.productId())
                            .orElseThrow(NotFoundException.supplier("상품"));
                    return new OrderService.OrderItemRequest(
                            item.productId(), item.qty(), product.getPrice());
                })
                .toList();
    }
}
