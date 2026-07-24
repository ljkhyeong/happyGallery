package com.personal.happygallery.application.order;

import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.order.port.in.AdminOrderClaimUseCase;
import com.personal.happygallery.application.order.port.in.OrderClaimUseCase;
import com.personal.happygallery.application.order.port.in.OrderClaimView;
import com.personal.happygallery.application.order.port.out.OrderClaimItemPort;
import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderItemClaimedQuantity;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.InventoryService.InventoryAdjustment;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderClaim;
import com.personal.happygallery.domain.order.OrderClaimItem;
import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultOrderClaimService implements OrderClaimUseCase, AdminOrderClaimUseCase {

    private static final int MAX_ADMIN_PAGE_SIZE = 100;

    private final OrderReaderPort orderReader;
    private final MemberAccountGuard memberAccountGuard;
    private final OrderItemPort orderItemPort;
    private final OrderClaimPort orderClaimPort;
    private final OrderClaimItemPort orderClaimItemPort;
    private final RefundPort refundPort;
    private final RefundExecutionService refundExecutionService;
    private final InventoryService inventoryService;
    private final GuestTokenService guestTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DefaultOrderClaimService(OrderReaderPort orderReader,
                                    MemberAccountGuard memberAccountGuard,
                                    OrderItemPort orderItemPort,
                                    OrderClaimPort orderClaimPort,
                                    OrderClaimItemPort orderClaimItemPort,
                                    RefundPort refundPort,
                                    RefundExecutionService refundExecutionService,
                                    InventoryService inventoryService,
                                    GuestTokenService guestTokenService,
                                    ApplicationEventPublisher eventPublisher,
                                    Clock clock) {
        this.orderReader = orderReader;
        this.memberAccountGuard = memberAccountGuard;
        this.orderItemPort = orderItemPort;
        this.orderClaimPort = orderClaimPort;
        this.orderClaimItemPort = orderClaimItemPort;
        this.refundPort = refundPort;
        this.refundExecutionService = refundExecutionService;
        this.inventoryService = inventoryService;
        this.guestTokenService = guestTokenService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public OrderClaimView requestMemberClaim(Long orderId, Long userId, RequestCommand command) {
        memberAccountGuard.requireActiveForUpdate(userId);
        Order order = requireOrderForUpdate(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new NotFoundException("주문");
        }
        return request(order, command);
    }

    @Override
    public OrderClaimView requestGuestClaim(Long orderId, String accessToken, RequestCommand command) {
        Order order = requireOrderForUpdate(orderId);
        if (!Objects.equals(order.getAccessToken(), guestTokenService.resolveTokenHash(accessToken))) {
            throw new NotFoundException("주문");
        }
        return request(order, command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderClaimView> listMemberClaims(Long orderId, Long userId) {
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new NotFoundException("주문");
        }
        return views(orderClaimPort.findByOrderIdOrderByRequestedAtDesc(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderClaimView> listGuestClaims(Long orderId, String accessToken) {
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getAccessToken(), guestTokenService.resolveTokenHash(accessToken))) {
            throw new NotFoundException("주문");
        }
        return views(orderClaimPort.findByOrderIdOrderByRequestedAtDesc(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderClaimView> list(OrderClaimStatus status, int size) {
        int limit = Math.clamp(size, 1, MAX_ADMIN_PAGE_SIZE);
        List<OrderClaim> claims = status == null
                ? orderClaimPort.findRecent(limit)
                : orderClaimPort.findRecentByStatus(status, limit);
        return views(claims);
    }

    @Override
    public OrderClaimView resolve(Long claimId, Long adminId, ResolveCommand command) {
        OrderClaim claim = requireClaimForUpdate(claimId);
        Order order = requireOrderForUpdate(claim.getOrderId());
        List<ClaimLine> lines = claimLines(claim);

        if (!command.approved()) {
            claim.reject(adminId, command.note(), now());
            OrderClaim saved = orderClaimPort.save(claim);
            notifyCustomer(order, saved, NotificationEventType.ORDER_CLAIM_RESOLVED);
            return view(saved);
        }

        List<InventoryAdjustment> inventoryAdjustments = lines.stream()
                .map(line -> new InventoryAdjustment(
                        line.orderItem().getProductId(), line.claimItem().getQuantity()))
                .toList();
        if (claim.getRequestedResolution() == OrderClaimResolution.EXCHANGE) {
            if (command.restoreInventory()) {
                inventoryService.restoreAll(inventoryAdjustments);
            }
            inventoryService.deductAll(inventoryAdjustments);
            claim.approveExchange(adminId, command.note(), now());
            OrderClaim saved = orderClaimPort.save(claim);
            notifyCustomer(order, saved, NotificationEventType.ORDER_CLAIM_RESOLVED);
            return view(saved);
        }

        if (command.restoreInventory()) {
            inventoryService.restoreAll(inventoryAdjustments);
        }
        long refundAmount = requireRefundAmount(command.refundAmount(), maximumRefundAmount(order, lines));
        allocateRefundAmount(lines, refundAmount);
        orderClaimItemPort.saveAll(lines.stream().map(ClaimLine::claimItem).toList());
        claim.approveRefund(adminId, command.note(), now());
        OrderClaim saved = orderClaimPort.save(claim);
        refundExecutionService.requestOrderClaimRefund(
                order.getId(), claim.getId(), refundAmount, order.getPaymentKey());
        notifyCustomer(order, saved, NotificationEventType.ORDER_CLAIM_RESOLVED);
        return view(saved);
    }

    @Override
    public OrderClaimView completeExchange(
            Long claimId, Long adminId, CompleteExchangeCommand command) {
        OrderClaim claim = requireClaimForUpdate(claimId);
        Order order = requireOrder(claim.getOrderId());
        claim.completeExchange(
                adminId, command.carrier(), command.trackingNumber(), command.note(), now());
        OrderClaim saved = orderClaimPort.save(claim);
        notifyCustomer(order, saved, NotificationEventType.ORDER_EXCHANGE_COMPLETED);
        return view(saved);
    }

    private OrderClaimView request(Order order, RequestCommand command) {
        if (command == null) {
            throw invalid("클레임 요청을 확인해주세요.");
        }
        order.getStatus().requireClaimable();
        List<OrderItem> orderItems = orderItemPort.findByOrder(order);
        List<RequestedLine> requestedLines = validateRequestedLines(command.items(), orderItems);

        OrderClaim claim = orderClaimPort.save(OrderClaim.request(
                order.getId(), command.type(), command.requestedResolution(), command.reason(), now()));
        orderClaimItemPort.saveAll(requestedLines.stream()
                .map(line -> new OrderClaimItem(
                        claim.getId(), order.getId(), line.item().getId(), line.quantity()))
                .toList());
        return view(claim);
    }

    private List<RequestedLine> validateRequestedLines(
            List<OrderClaimUseCase.Item> requestedItems, List<OrderItem> orderItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw invalid("클레임 상품을 하나 이상 선택해주세요.");
        }
        Map<Long, OrderItem> orderItemsById = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<Long, Integer> quantitiesByItemId = new LinkedHashMap<>();
        for (OrderClaimUseCase.Item requested : requestedItems) {
            if (requested == null || requested.orderItemId() == null || requested.quantity() <= 0) {
                throw invalid("클레임 상품과 수량을 확인해주세요.");
            }
            if (quantitiesByItemId.putIfAbsent(requested.orderItemId(), requested.quantity()) != null) {
                throw invalid("같은 주문 상품을 중복 선택할 수 없습니다.");
            }
        }

        Map<Long, Long> claimedByItemId = orderClaimItemPort
                .sumNonRejectedQuantities(quantitiesByItemId.keySet()).stream()
                .collect(Collectors.toMap(
                        OrderItemClaimedQuantity::orderItemId,
                        OrderItemClaimedQuantity::quantity));

        return quantitiesByItemId.entrySet().stream()
                .map(entry -> {
                    OrderItem item = orderItemsById.get(entry.getKey());
                    if (item == null) {
                        throw invalid("해당 주문에 포함되지 않은 상품입니다.");
                    }
                    long remaining = item.getQty() - claimedByItemId.getOrDefault(item.getId(), 0L);
                    if (entry.getValue() > remaining) {
                        throw invalid("이미 접수된 수량을 제외한 주문 수량을 초과할 수 없습니다.");
                    }
                    return new RequestedLine(item, entry.getValue());
                })
                .toList();
    }

    private long requireRefundAmount(Long requestedAmount, long maximumAmount) {
        if (requestedAmount == null || requestedAmount <= 0L || requestedAmount > maximumAmount) {
            throw invalid("환불액은 1원 이상이며 클레임 가능 금액을 넘을 수 없습니다.");
        }
        return requestedAmount;
    }

    private void allocateRefundAmount(List<ClaimLine> lines, long refundAmount) {
        long productAmount = lines.stream()
                .mapToLong(ClaimLine::amount)
                .reduce(0L, Math::addExact);
        long allocatableAmount = Math.min(refundAmount, productAmount);
        BigInteger productTotal = BigInteger.valueOf(productAmount);
        BigInteger allocationTotal = BigInteger.valueOf(allocatableAmount);

        List<RefundAllocation> allocations = lines.stream()
                .map(line -> {
                    BigInteger[] result = allocationTotal
                            .multiply(BigInteger.valueOf(line.amount()))
                            .divideAndRemainder(productTotal);
                    return new RefundAllocation(
                            line.claimItem(), result[0].longValueExact(), result[1]);
                })
                .toList();
        long unallocatedWon = allocatableAmount
                - allocations.stream().mapToLong(RefundAllocation::amount).sum();
        List<RefundAllocation> byLargestRemainder = new ArrayList<>(allocations);
        byLargestRemainder.sort(Comparator
                .comparing(RefundAllocation::remainder, Comparator.reverseOrder())
                .thenComparing(allocation -> allocation.item().getOrderItemId()));

        for (int index = 0; index < byLargestRemainder.size(); index++) {
            RefundAllocation allocation = byLargestRemainder.get(index);
            allocation.item().allocateApprovedRefundAmount(
                    allocation.amount() + (index < unallocatedWon ? 1L : 0L));
        }
    }

    private List<OrderClaimView> views(List<OrderClaim> claims) {
        if (claims.isEmpty()) {
            return List.of();
        }
        List<Long> claimIds = claims.stream().map(OrderClaim::getId).toList();
        List<OrderClaimItem> claimItems = orderClaimItemPort.findByClaimIdIn(claimIds);
        List<Long> orderIds = claims.stream().map(OrderClaim::getOrderId).distinct().toList();
        List<OrderItem> allOrderItems = orderItemPort.findByOrderIdIn(orderIds);
        Map<Long, OrderItem> orderItemsById = allOrderItems.stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<Long, List<OrderItem>> orderItemsByOrderId = allOrderItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
        Map<Long, List<OrderClaimItem>> claimItemsByClaimId = claimItems.stream()
                .collect(Collectors.groupingBy(OrderClaimItem::getClaimId));
        Map<Long, Order> ordersById = orderReader.findByIdIn(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        Map<Long, Refund> refundsByClaimId = refundPort.findByOrderClaimIdIn(claimIds).stream()
                .collect(Collectors.toMap(Refund::getOrderClaimId, Function.identity()));

        return claims.stream()
                .map(claim -> toView(
                        claim,
                        ordersById.get(claim.getOrderId()),
                        claimItemsByClaimId.getOrDefault(claim.getId(), List.of()),
                        orderItemsById,
                        orderItemsByOrderId.getOrDefault(claim.getOrderId(), List.of()),
                        refundsByClaimId.get(claim.getId())))
                .toList();
    }

    private OrderClaimView view(OrderClaim claim) {
        return views(List.of(claim)).getFirst();
    }

    private OrderClaimView toView(OrderClaim claim,
                                  Order order,
                                  List<OrderClaimItem> claimItems,
                                  Map<Long, OrderItem> orderItemsById,
                                  List<OrderItem> allOrderItems,
                                  Refund refund) {
        List<ClaimLine> lines = claimItems.stream()
                .map(item -> new ClaimLine(item, requireOrderItem(orderItemsById, item.getOrderItemId())))
                .toList();
        return new OrderClaimView(
                claim.getId(),
                claim.getOrderId(),
                claim.getType(),
                claim.getRequestedResolution(),
                claim.getStatus(),
                claim.getCustomerReason(),
                claim.getAdminNote(),
                claim.getResolvedByAdminId(),
                claim.getCompletedByAdminId(),
                claim.getReplacementCarrier(),
                claim.getReplacementTrackingNumber(),
                maximumRefundAmount(order, lines, allOrderItems),
                refund == null ? null : refund.getAmount(),
                refund == null ? null : refund.getStatus(),
                claim.getRequestedAt(),
                claim.getResolvedAt(),
                claim.getCompletedAt(),
                lines.stream()
                        .map(line -> new OrderClaimView.Item(
                                line.orderItem().getId(),
                                line.orderItem().getProductId(),
                                line.orderItem().getProductName(),
                                line.claimItem().getQuantity(),
                                line.orderItem().getUnitPrice()))
                        .toList());
    }

    private List<ClaimLine> claimLines(OrderClaim claim) {
        List<OrderClaimItem> claimItems = orderClaimItemPort.findByClaimIdIn(List.of(claim.getId()));
        Map<Long, OrderItem> orderItemsById = orderItemPort.findByIdIn(
                        claimItems.stream().map(OrderClaimItem::getOrderItemId).toList()).stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        return claimItems.stream()
                .map(item -> new ClaimLine(item, requireOrderItem(orderItemsById, item.getOrderItemId())))
                .toList();
    }

    private long maximumRefundAmount(Order order, List<ClaimLine> lines) {
        return maximumRefundAmount(order, lines, orderItemPort.findByOrder(order));
    }

    private long maximumRefundAmount(
            Order order, List<ClaimLine> lines, List<OrderItem> allOrderItems) {
        long itemAmount = lines.stream()
                .mapToLong(line -> Math.multiplyExact(
                        line.orderItem().getUnitPrice(), line.claimItem().getQuantity()))
                .sum();
        Map<Long, Integer> claimedQuantityByItemId = new HashMap<>();
        lines.forEach(line -> claimedQuantityByItemId.put(
                line.orderItem().getId(), line.claimItem().getQuantity()));
        boolean fullOrderClaim = allOrderItems.stream()
                .allMatch(item -> claimedQuantityByItemId.getOrDefault(item.getId(), 0) == item.getQty());
        return Math.addExact(itemAmount, fullOrderClaim ? order.getShippingFee() : 0L);
    }

    private Order requireOrder(Long orderId) {
        return orderReader.findById(orderId).orElseThrow(NotFoundException.supplier("주문"));
    }

    private Order requireOrderForUpdate(Long orderId) {
        return orderReader.findByIdForUpdate(orderId)
                .orElseThrow(NotFoundException.supplier("주문"));
    }

    private OrderClaim requireClaimForUpdate(Long claimId) {
        return orderClaimPort.findByIdForUpdate(claimId)
                .orElseThrow(NotFoundException.supplier("주문 클레임"));
    }

    private OrderItem requireOrderItem(Map<Long, OrderItem> itemsById, Long orderItemId) {
        OrderItem item = itemsById.get(orderItemId);
        if (item == null) {
            throw new NotFoundException("주문 상품");
        }
        return item;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private void notifyCustomer(
            Order order, OrderClaim claim, NotificationEventType eventType) {
        NotificationRequestedEvent event = order.getUserId() != null
                ? NotificationRequestedEvent.forUser(
                        order.getUserId(), eventType, "ORDER_CLAIM", claim.getId())
                : NotificationRequestedEvent.forGuest(
                        order.getGuestId(), eventType, "ORDER_CLAIM", claim.getId());
        eventPublisher.publishEvent(event);
    }

    private HappyGalleryException invalid(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }

    private record RequestedLine(OrderItem item, int quantity) {}

    private record ClaimLine(OrderClaimItem claimItem, OrderItem orderItem) {

        private long amount() {
            return Math.multiplyExact(orderItem.getUnitPrice(), claimItem.getQuantity());
        }
    }

    private record RefundAllocation(
            OrderClaimItem item, long amount, BigInteger remainder) {}
}
