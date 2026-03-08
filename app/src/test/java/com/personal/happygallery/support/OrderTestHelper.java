package com.personal.happygallery.support;

import com.personal.happygallery.app.order.OrderService;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderTestHelper {

    public record OrderFixture(Product product, Order order) {}

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;
    private final Clock clock;

    public OrderTestHelper(ProductRepository productRepository,
                           InventoryRepository inventoryRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           OrderService orderService) {
        this(productRepository, inventoryRepository, orderRepository, orderItemRepository, orderService, null);
    }

    public OrderTestHelper(ProductRepository productRepository,
                           InventoryRepository inventoryRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           OrderService orderService,
                           Clock clock) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
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
        Product product = productRepository.save(new Product(name, type, price));
        inventoryRepository.save(new Inventory(product, quantity));
        return product;
    }

    private OrderFixture createPaidOrder(String name, ProductType type, long price) {
        Product product = createProduct(name, type, price, 1);
        Order order = orderService.createPaidOrder(
                null,
                List.of(new OrderService.OrderItemRequest(product.getId(), 1, price)));
        return new OrderFixture(product, order);
    }

    private OrderFixture createExpiredPendingOrder(String name, ProductType type, long price) {
        if (clock == null) {
            throw new IllegalStateException("Expired order fixtures require a Clock");
        }
        Product product = createProduct(name, type, price, 1);
        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderRepository.save(new Order(null, price, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(order, product.getId(), 1, price));

        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        inventory.deduct(1);
        inventoryRepository.save(inventory);
        return new OrderFixture(product, order);
    }
}
