package com.personal.happygallery.application.order;

import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.order.port.in.AdminOrderClaimUseCase;
import com.personal.happygallery.application.order.port.in.OrderClaimUseCase;
import com.personal.happygallery.application.order.port.in.OrderClaimView;
import com.personal.happygallery.application.order.port.out.OrderClaimItemPort;
import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderItemClaimedQuantity;
import com.personal.happygallery.application.order.port.out.OrderItemApprovedRefundState;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.InventoryService.InventoryAdjustment;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.application.reward.RewardBenefitService.RewardEarnedSnapshot;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.application.token.GuestTokenService;
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
import java.time.Clock;
import java.time.LocalDateTime;
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

    private final OrderReaderPort orderReader;
    private final MemberAccountGuard memberAccountGuard;
    private final OrderItemPort orderItemPort;
    private final OrderClaimPort orderClaimPort;
    private final OrderClaimItemPort orderClaimItemPort;
    private final OrderClaimViewAssembler viewAssembler;
    private final RefundExecutionService refundExecutionService;
    private final RefundPort refundPort;
    private final RewardBenefitService rewardBenefitService;
    private final InventoryService inventoryService;
    private final GuestTokenService guestTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DefaultOrderClaimService(OrderReaderPort orderReader,
                                    MemberAccountGuard memberAccountGuard,
                                    OrderItemPort orderItemPort,
                                    OrderClaimPort orderClaimPort,
                                    OrderClaimItemPort orderClaimItemPort,
                                    OrderClaimViewAssembler viewAssembler,
                                    RefundExecutionService refundExecutionService,
                                    RefundPort refundPort,
                                    RewardBenefitService rewardBenefitService,
                                    InventoryService inventoryService,
                                    GuestTokenService guestTokenService,
                                    ApplicationEventPublisher eventPublisher,
                                    Clock clock) {
        this.orderReader = orderReader;
        this.memberAccountGuard = memberAccountGuard;
        this.orderItemPort = orderItemPort;
        this.orderClaimPort = orderClaimPort;
        this.orderClaimItemPort = orderClaimItemPort;
        this.viewAssembler = viewAssembler;
        this.refundExecutionService = refundExecutionService;
        this.refundPort = refundPort;
        this.rewardBenefitService = rewardBenefitService;
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
        return viewAssembler.assemble(
                orderClaimPort.findByOrderIdOrderByRequestedAtDesc(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderClaimView> listGuestClaims(Long orderId, String accessToken) {
        Order order = requireOrder(orderId);
        if (!Objects.equals(order.getAccessToken(), guestTokenService.resolveTokenHash(accessToken))) {
            throw new NotFoundException("주문");
        }
        return viewAssembler.assemble(
                orderClaimPort.findByOrderIdOrderByRequestedAtDesc(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<OrderClaimView> list(OrderClaimStatus status, String cursor, int size) {
        int pageSize = PageParams.clampSize(size);
        int fetchSize = pageSize + 1;
        List<OrderClaim> claims;
        if (cursor == null) {
            claims = status == null
                    ? orderClaimPort.findRecent(fetchSize)
                    : orderClaimPort.findRecentByStatus(status, fetchSize);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            claims = status == null
                    ? orderClaimPort.findRecentAfter(
                            cursorParam.timestamp(), cursorParam.id(), fetchSize)
                    : orderClaimPort.findRecentByStatusAfter(
                            status, cursorParam.timestamp(), cursorParam.id(), fetchSize);
        }
        return CursorPage.of(
                viewAssembler.assemble(claims),
                pageSize,
                claim -> CursorUtils.encode(claim.requestedAt(), claim.id()));
    }

    @Override
    public OrderClaimView resolve(Long claimId, Long adminId, ResolveCommand command) {
        OrderClaim claim = requireClaimForUpdate(claimId);
        Order order = requireOrderForUpdate(claim.getOrderId());
        List<OrderClaimLine> lines = claimLines(claim);

        if (!command.approved()) {
            claim.reject(adminId, command.note(), now());
            OrderClaim saved = orderClaimPort.save(claim);
            notifyCustomer(order, saved, NotificationEventType.ORDER_CLAIM_RESOLVED);
            return viewAssembler.assemble(saved);
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
            return viewAssembler.assemble(saved);
        }

        if (command.restoreInventory()) {
            inventoryService.restoreAll(inventoryAdjustments);
        }
        List<OrderItem> allOrderItems = orderItemPort.findByOrder(order);
        Map<Long, OrderItemApprovedRefundState> approvedStateByItemId =
                approvedRefundStateByItemId(allOrderItems);
        lines = lines.stream()
                .map(line -> new OrderClaimLine(
                        line.claimItem(),
                        line.orderItem(),
                        approvedStateByItemId.getOrDefault(
                                line.orderItem().getId(), emptyApprovedState(line.orderItem().getId()))
                                .quantity()))
                .toList();
        boolean fullOrderClaim = isFullOrderRefundClaim(
                lines, allOrderItems, approvedStateByItemId);
        long maximumRefundAmount = OrderClaimRefundCalculator.maximumRefundAmount(
                order, lines, fullOrderClaim);
        long refundAmount = requireRefundAmount(command.refundAmount(), maximumRefundAmount);
        long previousProductPgRefundAmount = approvedStateByItemId.values().stream()
                .mapToLong(OrderItemApprovedRefundState::pgRefundAmount)
                .reduce(0L, Math::addExact);
        RewardEarnedSnapshot earnedSnapshot = order.getUserId() == null
                ? RewardEarnedSnapshot.none()
                : rewardBenefitService.getEarnedSnapshot(order.getId());
        long reservedRewardRevokeAmount = Math.max(
                earnedSnapshot.revokedAmount(),
                refundPort.sumRewardRevokeAmountByOrderId(order.getId()));
        var allocation = OrderClaimRefundCalculator.allocateRefundAmount(
                order,
                lines,
                refundAmount,
                previousProductPgRefundAmount,
                earnedSnapshot.earnedAmount(),
                reservedRewardRevokeAmount);
        validateCumulativeBenefits(order, approvedStateByItemId, allocation);
        orderClaimItemPort.saveAll(lines.stream().map(OrderClaimLine::claimItem).toList());
        claim.approveRefund(adminId, command.note(), now());
        OrderClaim saved = orderClaimPort.save(claim);
        refundExecutionService.requestOrderClaimRefund(
                order.getId(),
                claim.getId(),
                allocation.pgRefundAmount(),
                allocation.customerRefundAmount(),
                allocation.rewardRestoreAmount(),
                allocation.rewardRevokeAmount(),
                order.getPaymentKey());
        notifyCustomer(order, saved, NotificationEventType.ORDER_CLAIM_RESOLVED);
        return viewAssembler.assemble(saved);
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
        return viewAssembler.assemble(saved);
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
        return viewAssembler.assemble(claim);
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

    private List<OrderClaimLine> claimLines(OrderClaim claim) {
        List<OrderClaimItem> claimItems = orderClaimItemPort.findByClaimIdIn(List.of(claim.getId()));
        Map<Long, OrderItem> orderItemsById = orderItemPort.findByIdIn(
                        claimItems.stream().map(OrderClaimItem::getOrderItemId).toList()).stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        return claimItems.stream()
                .map(item -> new OrderClaimLine(
                        item, requireOrderItem(orderItemsById, item.getOrderItemId())))
                .toList();
    }

    private Map<Long, OrderItemApprovedRefundState> approvedRefundStateByItemId(
            List<OrderItem> allOrderItems) {
        if (allOrderItems.isEmpty()) {
            return Map.of();
        }
        return orderClaimItemPort.summarizeApprovedRefunds(
                        allOrderItems.stream().map(OrderItem::getId).toList()).stream()
                .collect(Collectors.toMap(
                        OrderItemApprovedRefundState::orderItemId,
                        Function.identity()));
    }

    private boolean isFullOrderRefundClaim(
            List<OrderClaimLine> lines,
            List<OrderItem> allOrderItems,
            Map<Long, OrderItemApprovedRefundState> approvedStateByItemId) {
        Map<Long, Integer> currentQuantityByItemId = lines.stream()
                .collect(Collectors.toMap(
                        line -> line.orderItem().getId(),
                        line -> line.claimItem().getQuantity()));
        return allOrderItems.stream().allMatch(item -> {
            long approvedQuantity = approvedStateByItemId.getOrDefault(
                    item.getId(), emptyApprovedState(item.getId())).quantity();
            return approvedQuantity + currentQuantityByItemId.getOrDefault(item.getId(), 0)
                    == item.getQty();
        });
    }

    private void validateCumulativeBenefits(
            Order order,
            Map<Long, OrderItemApprovedRefundState> approvedStateByItemId,
            OrderClaimRefundCalculator.RefundAllocation allocation) {
        long previousCustomerRefundAmount = approvedStateByItemId.values().stream()
                .mapToLong(OrderItemApprovedRefundState::customerRefundAmount)
                .reduce(0L, Math::addExact);
        long previousRewardRestoreAmount = approvedStateByItemId.values().stream()
                .mapToLong(OrderItemApprovedRefundState::rewardRestoreAmount)
                .reduce(0L, Math::addExact);
        long currentProductCustomerRefund = allocation.customerRefundAmount()
                - allocation.shippingRefundAmount();
        long refundableProductAmount = Math.addExact(
                order.getRewardEarnBase(), order.getRewardUsedAmount());
        if (Math.addExact(previousCustomerRefundAmount, currentProductCustomerRefund)
                    > refundableProductAmount
                || Math.addExact(previousRewardRestoreAmount, allocation.rewardRestoreAmount())
                    > order.getRewardUsedAmount()) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "클레임 환불 누계가 주문의 결제·혜택 금액을 초과합니다.");
        }
    }

    private OrderItemApprovedRefundState emptyApprovedState(Long orderItemId) {
        return new OrderItemApprovedRefundState(orderItemId, 0L, 0L, 0L);
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

}
