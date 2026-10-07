package com.personal.happygallery.support;

import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.order.OrderService.OrderItemRequest;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderItemPricing;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** 주문 통합 테스트용 상품·회원·주문 fixture. 전체 Spring context에서 빈으로 주입받는다. */
@Component
public class OrderTestHelper {

    private static final AtomicLong USER_SEQUENCE = new AtomicLong();

    public record OrderFixture(Product product, Order order) {}

    private final ProductStorePort productStorePort;
    private final InventoryStorePort inventoryStorePort;
    private final InventoryReaderPort inventoryReaderPort;
    private final OrderStorePort orderStorePort;
    private final OrderItemPort orderItemPort;
    private final UserStorePort userStorePort;
    private final OrderService orderService;
    private final Clock clock;

    public OrderTestHelper(ProductStorePort productStorePort,
                           InventoryStorePort inventoryStorePort,
                           InventoryReaderPort inventoryReaderPort,
                           OrderStorePort orderStorePort,
                           OrderItemPort orderItemPort,
                           UserStorePort userStorePort,
                           OrderService orderService,
                           Clock clock) {
        this.productStorePort = productStorePort;
        this.inventoryStorePort = inventoryStorePort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.orderStorePort = orderStorePort;
        this.orderItemPort = orderItemPort;
        this.userStorePort = userStorePort;
        this.orderService = orderService;
        this.clock = clock;
    }

    /** 상품의 유형·고정 사양·관리 방법·제작 기간을 그대로 담은 전액 결제 주문 항목을 만든다. */
    public static OrderItemRequest item(Product product, int qty, long unitPrice) {
        return new OrderItemRequest(
                product.getId(), null, product.getName(), product.getType(),
                qty, unitPrice, unitPrice, 0L, 0L, List.of(),
                product.getSpecification(), product.getCareInstructions(), product.getProductionLeadDays(),
                OrderItemPricing.fullPrice(qty, unitPrice));
    }

    /** 혜택 없이 전액 결제한 회원 주문을 만든다. 가격 스냅샷은 항목 합계와 배송비로 계산한다. */
    public Order createMemberOrder(Long userId,
                                   List<OrderItemRequest> items,
                                   FulfillmentType fulfillmentType,
                                   ShippingAddress shippingAddress,
                                   long shippingFee,
                                   MadeToOrderConsent madeToOrderConsent) {
        long productAmount = 0L;
        for (OrderItemRequest item : items) {
            productAmount = OrderAmountCalculator.addLine(productAmount, item.qty(), item.unitPrice());
        }
        return orderService.createMemberOrder(
                userId, items, fulfillmentType, shippingAddress, madeToOrderConsent,
                OrderPricingSnapshot.fullPrice(productAmount, shippingFee));
    }

    /** 혜택 없이 전액 결제한 회원 픽업 주문을 만든다. */
    public Order createMemberPickupOrder(Long userId, List<OrderItemRequest> items) {
        return createMemberOrder(userId, items, FulfillmentType.PICKUP, null, 0L, null);
    }

    public Product createReadyStockProduct(String name, long price, int quantity) {
        return createProduct(name, ProductType.READY_STOCK, price, quantity);
    }

    public OrderFixture createReadyStockPaidOrder(String name, long price) {
        return createPaidOrder(name, ProductType.READY_STOCK, price);
    }

    public OrderFixture createMadeToOrderPaidOrder(String name, long price) {
        return createPaidOrder(name, ProductType.MADE_TO_ORDER, price);
    }

    public OrderFixture createMadeToOrderPaidShippingOrder(String name, long price) {
        return createPaidOrder(name, ProductType.MADE_TO_ORDER, price, FulfillmentType.SHIPPING);
    }

    public OrderFixture createReadyStockPaidShippingOrder(String name, long price, long shippingFee) {
        return createPaidOrder(name, ProductType.READY_STOCK, price, FulfillmentType.SHIPPING, shippingFee);
    }

    public OrderFixture createExpiredReadyStockPendingOrder(String name, long price) {
        return createExpiredPendingOrder(name, ProductType.READY_STOCK, price);
    }

    public OrderFixture createExpiredMadeToOrderPendingOrder(String name, long price) {
        return createExpiredPendingOrder(name, ProductType.MADE_TO_ORDER, price);
    }

    public User createMemberOwner() {
        long sequence = USER_SEQUENCE.incrementAndGet();
        return userStorePort.save(new User(
                "order-fixture-%d@test.local".formatted(sequence),
                "password-hash",
                "주문 테스트 회원",
                "010%08d".formatted(sequence % 100_000_000)));
    }

    private Product createProduct(String name, ProductType type, long price, int quantity) {
        Product product = productStorePort.save(type == ProductType.MADE_TO_ORDER
                ? new Product(
                        name, type, null, price, null, null,
                        "재료: 테스트 재료\n크기: 테스트 규격\n사양: 고정 사양",
                        "직사광선을 피해 보관하세요.",
                        14)
                : new Product(name, type, price));
        inventoryStorePort.save(new Inventory(product, quantity));
        return product;
    }

    private OrderFixture createPaidOrder(String name, ProductType type, long price) {
        return createPaidOrder(name, type, price, FulfillmentType.PICKUP);
    }

    private OrderFixture createPaidOrder(
            String name, ProductType type, long price, FulfillmentType fulfillmentType) {
        return createPaidOrder(name, type, price, fulfillmentType, 0L);
    }

    private OrderFixture createPaidOrder(
            String name, ProductType type, long price, FulfillmentType fulfillmentType, long shippingFee) {
        Product product = createProduct(name, type, price, 1);
        User member = createMemberOwner();
        Order order = createMemberOrder(
                member.getId(),
                List.of(item(product, 1, price)),
                fulfillmentType,
                fulfillmentType == FulfillmentType.SHIPPING
                        ? new ShippingAddress("주문 테스트 회원", "01012345678", "06236", "서울시 강남구 테헤란로 1", null)
                        : null,
                shippingFee,
                type == ProductType.MADE_TO_ORDER
                        ? MadeToOrderConsent.current(LocalDateTime.of(2026, 1, 1, 0, 0))
                        : null);
        return new OrderFixture(product, order);
    }

    private OrderFixture createExpiredPendingOrder(String name, ProductType type, long price) {
        Product product = createProduct(name, type, price, 1);
        User member = createMemberOwner();
        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderStorePort.save(
                Order.forMember(
                        member.getId(), price, 0L, paidAt, paidAt.plusHours(24),
                        type == ProductType.MADE_TO_ORDER
                                ? MadeToOrderConsent.current(paidAt)
                                : null));
        orderItemPort.save(new OrderItem(
                order,
                product.getId(),
                null,
                product.getName(),
                product.getType(),
                1,
                price,
                price,
                0L,
                0L,
                List.of(),
                product.getSpecification(),
                product.getCareInstructions(),
                product.getProductionLeadDays(),
                OrderItemPricing.fullPrice(1, price)));

        Inventory inventory = inventoryReaderPort.findByProductId(product.getId()).orElseThrow();
        inventory.deduct(1);
        inventoryStorePort.save(inventory);
        return new OrderFixture(product, order);
    }
}
