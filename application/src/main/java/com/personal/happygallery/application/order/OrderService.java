package com.personal.happygallery.application.order;

import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.InventoryService.InventoryAdjustment;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderItemPricing;
import com.personal.happygallery.domain.order.OrderPricingSnapshot;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentPolicy;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.product.ProductType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 생성 서비스.
 *
 * <p>결제 완료 시 호출. 재고를 차감하고 주문을 {@link com.personal.happygallery.domain.order.OrderStatus#PAID_APPROVAL_PENDING}
 * 상태로 생성한다. 승인 마감은 결제 시각 + 24시간.
 */
@Service
@Transactional
public class OrderService {

    private final OrderStorePort orderStore;
    private final OrderItemPort orderItemPort;
    private final FulfillmentPort fulfillmentPort;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final GuestTokenService guestTokenService;
    private final ShippingAddressProtector shippingAddressProtector;
    private final MemberAccountGuard memberAccountGuard;
    private final Clock clock;

    public OrderService(OrderStorePort orderStore,
                        OrderItemPort orderItemPort,
                        FulfillmentPort fulfillmentPort,
                        InventoryService inventoryService,
                        ApplicationEventPublisher eventPublisher,
                        GuestTokenService guestTokenService,
                        ShippingAddressProtector shippingAddressProtector,
                        MemberAccountGuard memberAccountGuard,
                        Clock clock) {
        this.orderStore = orderStore;
        this.orderItemPort = orderItemPort;
        this.fulfillmentPort = fulfillmentPort;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
        this.guestTokenService = guestTokenService;
        this.shippingAddressProtector = shippingAddressProtector;
        this.memberAccountGuard = memberAccountGuard;
        this.clock = clock;
    }

    /**
     * 결제 완료 주문을 생성한다.
     *
     * <ol>
     *   <li>각 상품의 재고를 차감한다 (재고 부족 시 {@link com.personal.happygallery.domain.error.InventoryNotEnoughException}).</li>
     *   <li>주문을 {@link com.personal.happygallery.domain.order.OrderStatus#PAID_APPROVAL_PENDING}으로 저장한다.</li>
     *   <li>승인 마감({@code approvalDeadlineAt})을 결제 시각 + 24시간으로 설정한다.</li>
     * </ol>
     *
     * @param guestId 비회원 ID (회원 주문은 null)
     * @param items   주문 상품 목록
     * @return 생성된 주문
     */
    public OrderCreationResult createPaidOrder(Long guestId, List<OrderItemRequest> items,
                                               FulfillmentType fulfillmentType,
                                               ShippingAddress shippingAddress) {
        return createPaidOrder(guestId, items, fulfillmentType, shippingAddress, 0L);
    }

    public OrderCreationResult createPaidOrder(Long guestId, List<OrderItemRequest> items,
                                               FulfillmentType fulfillmentType,
                                               ShippingAddress shippingAddress,
                                               long shippingFee) {
        return createPaidOrder(
                guestId, items, fulfillmentType, shippingAddress, shippingFee, null);
    }

    public OrderCreationResult createPaidOrder(Long guestId, List<OrderItemRequest> items,
                                               FulfillmentType fulfillmentType,
                                               ShippingAddress shippingAddress,
                                               long shippingFee,
                                               MadeToOrderConsent madeToOrderConsent) {
        LocalDateTime paidAt = LocalDateTime.now(clock);
        long totalAmount = OrderAmountCalculator.addShippingFee(
                productAmount(items), shippingFee);
        requireMatchingShippingFee(fulfillmentType, shippingFee);

        GuestTokenService.IssuedToken issued = guestTokenService.issue();
        String rawToken = issued.rawToken();
        String tokenHash = issued.tokenHash();
        Order order = orderStore.save(
                Order.forGuest(
                        guestId, tokenHash, totalAmount, shippingFee,
                        paidAt, paidAt.plusHours(24), madeToOrderConsent));

        saveItemsAndDeductInventory(order, items);
        saveFulfillment(order, fulfillmentType, shippingAddress);

        eventPublisher.publishEvent(NotificationRequestedEvent.forGuest(
                guestId,
                NotificationEventType.ORDER_PAID,
                "ORDER",
                order.getId()));

        return new OrderCreationResult(order, rawToken);
    }

    /**
     * 회원 주문 생성. guest 대신 user_id를 설정한다. accessToken 없음.
     */
    public Order createMemberOrder(Long userId, List<OrderItemRequest> items,
                                   FulfillmentType fulfillmentType,
                                   ShippingAddress shippingAddress) {
        return createMemberOrder(userId, items, fulfillmentType, shippingAddress, 0L);
    }

    public Order createMemberOrder(Long userId, List<OrderItemRequest> items,
                                   FulfillmentType fulfillmentType,
                                   ShippingAddress shippingAddress,
                                   long shippingFee) {
        return createMemberOrder(
                userId, items, fulfillmentType, shippingAddress, shippingFee, null);
    }

    public Order createMemberOrder(Long userId, List<OrderItemRequest> items,
                                   FulfillmentType fulfillmentType,
                                   ShippingAddress shippingAddress,
                                   long shippingFee,
                                   MadeToOrderConsent madeToOrderConsent) {
        return createMemberOrder(
                userId, items, fulfillmentType, shippingAddress, madeToOrderConsent,
                OrderPricingSnapshot.fullPrice(productAmount(items), shippingFee));
    }

    public Order createMemberOrder(Long userId, List<OrderItemRequest> items,
                                   FulfillmentType fulfillmentType,
                                   ShippingAddress shippingAddress,
                                   MadeToOrderConsent madeToOrderConsent,
                                   OrderPricingSnapshot pricing) {
        memberAccountGuard.requireActiveForUpdate(userId);
        LocalDateTime paidAt = LocalDateTime.now(clock);
        requirePricingMatchesItems(items, pricing);
        requireMatchingShippingFee(fulfillmentType, pricing.shippingFee());

        Order order = orderStore.save(
                Order.forMember(
                        userId, pricing,
                        paidAt, paidAt.plusHours(24), madeToOrderConsent));

        saveItemsAndDeductInventory(order, items);
        saveFulfillment(order, fulfillmentType, shippingAddress);

        eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                userId,
                NotificationEventType.ORDER_PAID,
                "ORDER",
                order.getId()));

        return order;
    }

    /** 테스트·내부 fixture용 기본 픽업 주문 생성 경로. 운영 결제는 수령 방법을 명시한다. */
    public OrderCreationResult createPaidOrder(Long guestId, List<OrderItemRequest> items) {
        return createPaidOrder(guestId, items, FulfillmentType.PICKUP, null);
    }

    /** 테스트·내부 fixture용 기본 픽업 주문 생성 경로. 운영 결제는 수령 방법을 명시한다. */
    public Order createMemberOrder(Long userId, List<OrderItemRequest> items) {
        return createMemberOrder(userId, items, FulfillmentType.PICKUP, null);
    }

    private void saveItemsAndDeductInventory(Order order, List<OrderItemRequest> items) {
        inventoryService.deductAll(items.stream()
                .map(item -> new InventoryAdjustment(item.productId(), item.qty()))
                .toList());
        orderItemPort.saveAll(items.stream()
                .map(item -> new OrderItem(
                        order,
                        item.productId(),
                        item.productName(),
                        item.productType(),
                        item.qty(),
                        item.unitPrice(),
                        item.specification(),
                        item.careInstructions(),
                        item.productionLeadDays(),
                        item.pricing()))
                .toList());
    }

    private static long productAmount(List<OrderItemRequest> items) {
        long total = 0L;
        for (OrderItemRequest item : items) {
            total = OrderAmountCalculator.addLine(total, item.qty(), item.unitPrice());
        }
        return total;
    }

    private static void requirePricingMatchesItems(
            List<OrderItemRequest> items, OrderPricingSnapshot pricing) {
        long productAmount = productAmount(items);
        long couponDiscount = 0L;
        long rewardUsed = 0L;
        long netPaid = 0L;
        for (OrderItemRequest item : items) {
            couponDiscount = Math.addExact(
                    couponDiscount, item.pricing().couponDiscountAmount());
            rewardUsed = Math.addExact(rewardUsed, item.pricing().rewardUsedAmount());
            netPaid = Math.addExact(netPaid, item.pricing().netPaidAmount());
        }
        if (pricing.productAmount() != productAmount
                || pricing.couponDiscountAmount() != couponDiscount
                || pricing.rewardUsedAmount() != rewardUsed
                || pricing.rewardEarnBase() != netPaid) {
            throw new IllegalArgumentException("주문 가격과 품목별 혜택 배분이 일치하지 않습니다.");
        }
    }

    private static void requireMatchingShippingFee(FulfillmentType fulfillmentType, long shippingFee) {
        if (fulfillmentType != FulfillmentType.SHIPPING && shippingFee != 0L) {
            throw new IllegalArgumentException("픽업 주문에는 배송비를 적용할 수 없습니다.");
        }
    }

    private void saveFulfillment(Order order,
                                 FulfillmentType fulfillmentType,
                                 ShippingAddress shippingAddress) {
        FulfillmentPolicy.requireValid(fulfillmentType, shippingAddress);
        Fulfillment fulfillment = switch (fulfillmentType) {
            case PICKUP -> Fulfillment.pickup(order.getId());
            case SHIPPING -> Fulfillment.shipping(
                    order.getId(), shippingAddressProtector.encrypt(shippingAddress));
        };
        fulfillmentPort.save(fulfillment);
    }

    public record OrderItemRequest(
            Long productId,
            String productName,
            ProductType productType,
            int qty,
            long unitPrice,
            String specification,
            String careInstructions,
            Integer productionLeadDays,
            OrderItemPricing pricing
    ) {
        public OrderItemRequest(Long productId, String productName, int qty, long unitPrice) {
            this(productId, productName, ProductType.READY_STOCK,
                    qty, unitPrice, null, null, null,
                    OrderItemPricing.fullPrice(qty, unitPrice));
        }

        public OrderItemRequest(
                Long productId,
                String productName,
                int qty,
                long unitPrice,
                String specification,
                String careInstructions,
                Integer productionLeadDays
        ) {
            this(productId, productName,
                    productionLeadDays == null ? ProductType.READY_STOCK : ProductType.MADE_TO_ORDER,
                    qty, unitPrice, specification, careInstructions, productionLeadDays,
                    OrderItemPricing.fullPrice(qty, unitPrice));
        }

        public OrderItemRequest(
                Long productId,
                String productName,
                ProductType productType,
                int qty,
                long unitPrice,
                String specification,
                String careInstructions,
                Integer productionLeadDays
        ) {
            this(productId, productName, productType, qty, unitPrice,
                    specification, careInstructions, productionLeadDays,
                    OrderItemPricing.fullPrice(qty, unitPrice));
        }

        public OrderItemRequest {
            if (productType == null) {
                throw new IllegalArgumentException("신규 주문 상품의 상품 유형은 필수입니다.");
            }
            if (pricing == null) {
                throw new IllegalArgumentException("주문 품목 가격 스냅샷은 필수입니다.");
            }
        }
    }

    public record OrderCreationResult(Order order, String rawAccessToken) {}
}
