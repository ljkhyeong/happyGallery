package com.personal.happygallery.support;

import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class OrderTestHelper {

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
                           OrderService orderService) {
        this(productStorePort, inventoryStorePort, inventoryReaderPort, orderStorePort, orderItemPort,
                userStorePort, orderService, null);
    }

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
        Product product = productStorePort.save(new Product(name, type, price));
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
        Order order = orderService.createMemberOrder(
                member.getId(),
                List.of(new OrderService.OrderItemRequest(product.getId(), product.getName(), 1, price)),
                fulfillmentType,
                fulfillmentType == FulfillmentType.SHIPPING
                        ? new ShippingAddress("주문 테스트 회원", "01012345678", "06236", "서울시 강남구 테헤란로 1", null)
                        : null,
                shippingFee);
        return new OrderFixture(product, order);
    }

    private OrderFixture createExpiredPendingOrder(String name, ProductType type, long price) {
        if (clock == null) {
            throw new IllegalStateException("Expired order fixtures require a Clock");
        }
        Product product = createProduct(name, type, price, 1);
        User member = createMemberOwner();
        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderStorePort.save(
                Order.forMember(member.getId(), price, paidAt, paidAt.plusHours(24)));
        orderItemPort.save(new OrderItem(order, product.getId(), product.getName(), 1, price));

        Inventory inventory = inventoryReaderPort.findByProductId(product.getId()).orElseThrow();
        inventory.deduct(1);
        inventoryStorePort.save(inventory);
        return new OrderFixture(product, order);
    }
}
