package com.personal.happygallery.support;

import com.personal.happygallery.application.order.OrderService;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderTestHelper {

    public record OrderFixture(Product product, Order order) {}

    private final ProductStorePort productStorePort;
    private final InventoryStorePort inventoryStorePort;
    private final InventoryReaderPort inventoryReaderPort;
    private final OrderStorePort orderStorePort;
    private final OrderItemPort orderItemPort;
    private final OrderService orderService;
    private final Clock clock;

    public OrderTestHelper(ProductStorePort productStorePort,
                           InventoryStorePort inventoryStorePort,
                           InventoryReaderPort inventoryReaderPort,
                           OrderStorePort orderStorePort,
                           OrderItemPort orderItemPort,
                           OrderService orderService) {
        this(productStorePort, inventoryStorePort, inventoryReaderPort, orderStorePort, orderItemPort, orderService, null);
    }

    public OrderTestHelper(ProductStorePort productStorePort,
                           InventoryStorePort inventoryStorePort,
                           InventoryReaderPort inventoryReaderPort,
                           OrderStorePort orderStorePort,
                           OrderItemPort orderItemPort,
                           OrderService orderService,
                           Clock clock) {
        this.productStorePort = productStorePort;
        this.inventoryStorePort = inventoryStorePort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.orderStorePort = orderStorePort;
        this.orderItemPort = orderItemPort;
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

    public OrderFixture createExpiredReadyStockPendingOrder(String name, long price) {
        return createExpiredPendingOrder(name, ProductType.READY_STOCK, price);
    }

    public OrderFixture createExpiredMadeToOrderPendingOrder(String name, long price) {
        return createExpiredPendingOrder(name, ProductType.MADE_TO_ORDER, price);
    }

    private Product createProduct(String name, ProductType type, long price, int quantity) {
        Product product = productStorePort.save(new Product(name, type, price));
        inventoryStorePort.save(new Inventory(product, quantity));
        return product;
    }

    private OrderFixture createPaidOrder(String name, ProductType type, long price) {
        Product product = createProduct(name, type, price, 1);
        var result = orderService.createPaidOrder(
                null,
                List.of(new OrderService.OrderItemRequest(product.getId(), 1, price)));
        return new OrderFixture(product, result.order());
    }

    private OrderFixture createExpiredPendingOrder(String name, ProductType type, long price) {
        if (clock == null) {
            throw new IllegalStateException("Expired order fixtures require a Clock");
        }
        Product product = createProduct(name, type, price, 1);
        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderStorePort.save(Order.forGuest(null, null, price, paidAt, paidAt.plusHours(24)));
        orderItemPort.save(new OrderItem(order, product.getId(), 1, price));

        Inventory inventory = inventoryReaderPort.findByProductId(product.getId()).orElseThrow();
        inventory.deduct(1);
        inventoryStorePort.save(inventory);
        return new OrderFixture(product, order);
    }
}
