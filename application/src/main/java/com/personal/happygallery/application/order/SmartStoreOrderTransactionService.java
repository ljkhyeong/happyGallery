package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderChange;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.AdminActor;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.InventoryResolutionCommand;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderActionHistoryPort;
import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.ProductVariantStockService;
import com.personal.happygallery.application.product.ProductVariantStockService.VariantAdjustment;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantReaderPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.application.product.port.out.SmartStoreOrderMappingHistoryPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreOrderActionHistory;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import com.personal.happygallery.domain.product.SmartStoreOrderMappingHistory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SmartStoreOrderTransactionService {

    private static final Set<String> ACTIVE_STOCK_STATUSES = Set.of(
            "PAYED", "DELIVERING", "DELIVERED", "PURCHASE_DECIDED", "EXCHANGED");
    private static final Set<String> CANCELED_STATUSES = Set.of(
            "CANCELED", "CANCELED_BY_NOPAYMENT");

    private final SmartStoreProductOrderPort orderPort;
    private final SmartStoreStockMappingPort mappingPort;
    private final SmartStoreOrderMappingHistoryPort orderMappingHistoryPort;
    private final SmartStoreOrderActionHistoryPort actionHistoryPort;
    private final ProductReaderPort productReaderPort;
    private final ProductVariantReaderPort variantReaderPort;
    private final InventoryService inventoryService;
    private final ProductVariantStockService variantStockService;
    private final SmartStoreDeliveryInfoProtector deliveryInfoProtector;
    private final Clock clock;

    SmartStoreOrderTransactionService(
            SmartStoreProductOrderPort orderPort,
            SmartStoreStockMappingPort mappingPort,
            SmartStoreOrderMappingHistoryPort orderMappingHistoryPort,
            SmartStoreOrderActionHistoryPort actionHistoryPort,
            ProductReaderPort productReaderPort,
            ProductVariantReaderPort variantReaderPort,
            InventoryService inventoryService,
            ProductVariantStockService variantStockService,
            SmartStoreDeliveryInfoProtector deliveryInfoProtector,
            Clock clock) {
        this.orderPort = orderPort;
        this.mappingPort = mappingPort;
        this.orderMappingHistoryPort = orderMappingHistoryPort;
        this.actionHistoryPort = actionHistoryPort;
        this.productReaderPort = productReaderPort;
        this.variantReaderPort = variantReaderPort;
        this.inventoryService = inventoryService;
        this.variantStockService = variantStockService;
        this.deliveryInfoProtector = deliveryInfoProtector;
        this.clock = clock;
    }

    @Transactional
    public SmartStoreProductOrder synchronize(ProductOrderDetail detail, ProductOrderChange change) {
        Optional<SmartStoreProductOrder> existing = orderPort
                .findByProductOrderIdWithLock(detail.productOrderId());
        SmartStoreProductOrder order = existing.orElseGet(() -> new SmartStoreProductOrder(
                detail.productOrderId(), detail.orderId(), detail.originProductNo(), detail.itemNo(),
                detail.productName(), detail.productOption(),
                deliveryInfoProtector.encrypt(detail.deliveryInfo()), detail.productOrderStatus(),
                detail.placeOrderStatus(), detail.claimType(), detail.claimStatus(),
                detail.initialQuantity(), detail.remainQuantity(), change.lastChangedType(),
                detail.paymentDate(), change.lastChangedAt(), detail.shippingDueDate(),
                detail.expectedDeliveryMethod(), detail.deliveryCompany(), detail.trackingNumber(),
                detail.unitPrice(), detail.paymentAmount(), detail.paymentCommission(),
                detail.saleCommission(), detail.channelCommission(), detail.expectedSettlementAmount(),
                detail.completedReturnQuantity()));
        if (existing.isPresent() && !order.refresh(
                detail.orderId(), detail.originProductNo(), detail.itemNo(), detail.productName(),
                detail.productOption(), deliveryInfoProtector.encrypt(detail.deliveryInfo()),
                detail.productOrderStatus(), detail.placeOrderStatus(), detail.claimType(),
                detail.claimStatus(), detail.initialQuantity(), detail.remainQuantity(),
                change.lastChangedType(), detail.paymentDate(), change.lastChangedAt(),
                detail.shippingDueDate(), detail.expectedDeliveryMethod(), detail.deliveryCompany(),
                detail.trackingNumber(), detail.unitPrice(), detail.paymentAmount(),
                detail.paymentCommission(), detail.saleCommission(), detail.channelCommission(),
                detail.expectedSettlementAmount(), detail.completedReturnQuantity(),
                detail.completedReturnQuantityAt(order.getLastChangedAt()))) {
            return order;
        }
        reconcile(order);
        return orderPort.save(order);
    }

    @Transactional
    public SmartStoreProductOrder retryInventory(String productOrderId) {
        SmartStoreProductOrder order = lockedOrder(productOrderId);
        reconcile(order);
        return orderPort.save(order);
    }

    @Transactional
    public SmartStoreProductOrder resolveReturn(String productOrderId, boolean restoreStock, String reviewVersion) {
        SmartStoreProductOrder order = lockedOrder(productOrderId);
        int restoreQuantity = order.resolveReturn(restoreStock, reviewVersion);
        if (restoreQuantity > 0) {
            restore(order, restoreQuantity);
        }
        return orderPort.save(order);
    }

    @Transactional
    public SmartStoreProductOrder resolveInventory(InventoryResolutionCommand command, AdminActor actor) {
        SmartStoreProductOrder order = lockedOrder(command.productOrderId());
        if (!Objects.equals(order.inventoryResolutionVersion(), command.expectedResolutionVersion())) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "주문 재고 확인 대상이 변경되었습니다. 최신 주문을 다시 확인해 주세요.");
        }
        if (order.getAttentionReason() != SmartStoreOrderAttentionReason.MAPPING_REQUIRED
                && order.getAttentionReason() != SmartStoreOrderAttentionReason.STATUS_REVIEW) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "수동 재고 결정이 필요한 스마트스토어 주문이 아닙니다.");
        }
        validateManualTarget(order, command.productId(), command.productVariantId());
        order.mapTo(command.productId(), command.productVariantId());

        int targetQuantity = switch (command.action()) {
            case APPLY_REMAINING -> order.targetInventoryQuantity(order.getRemainQuantity());
            case RESTORE_ALL -> 0;
            case KEEP_CURRENT -> order.getInventoryAppliedQuantity();
        };
        boolean applied = changeAppliedQuantity(order, targetQuantity);
        if (applied && order.pendingReturnQuantity() > 0) {
            order.requireAttention(SmartStoreOrderAttentionReason.RETURN_REVIEW);
        }
        SmartStoreProductOrder saved = orderPort.save(order);

        LocalDateTime changedAt = LocalDateTime.now(clock);
        String summary = "상품 %d, 옵션 조합 %s, 재고 결정 %s, 목표 적용 %d개, 사유: %s".formatted(
                command.productId(), Objects.toString(command.productVariantId(), "없음"),
                command.action(), targetQuantity, command.reason().strip());
        SmartStoreOrderActionHistory history = new SmartStoreOrderActionHistory(
                command.productOrderId(), SmartStoreOrderAction.INVENTORY_RESOLVED, summary,
                actor.adminUserId(), actor.name(), changedAt);
        if (applied) {
            history.succeed(changedAt);
        } else {
            history.reject("STOCK_SHORTAGE", "내부 재고가 부족해 선택한 수량을 반영하지 못했습니다.", changedAt);
        }
        actionHistoryPort.save(history);
        return saved;
    }

    private void validateManualTarget(
            SmartStoreProductOrder order,
            Long productId,
            Long productVariantId) {
        Product product = productReaderPort.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        if (product.getType() == ProductType.READY_STOCK && productVariantId != null) {
            throw new IllegalArgumentException("기성품은 옵션 조합 없이 연결해 주세요.");
        }
        if (product.getType() == ProductType.MADE_TO_ORDER) {
            if (productVariantId == null) {
                throw new IllegalArgumentException("주문제작 상품은 옵션 조합을 선택해 주세요.");
            }
            variantReaderPort.findWithSelectionsById(productVariantId)
                    .filter(found -> found.getProductId().equals(productId))
                    .orElseThrow(NotFoundException.supplier("상품 옵션 조합"));
        }
        boolean mappingChanged = !Objects.equals(order.getProductId(), productId)
                || !Objects.equals(order.getProductVariantId(), productVariantId);
        if (mappingChanged && order.getInventoryAppliedQuantity() > 0) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "이미 재고가 반영된 주문은 다른 상품이나 옵션 조합으로 변경할 수 없습니다.");
        }
    }

    private void reconcile(SmartStoreProductOrder order) {
        String status = order.getProductOrderStatus();
        boolean canceled = CANCELED_STATUSES.contains(status) || "PAYMENT_WAITING".equals(status);
        if (!canceled && !"RETURNED".equals(status) && !ACTIVE_STOCK_STATUSES.contains(status)) {
            if (ensureMapping(order)) {
                order.requireAttention(SmartStoreOrderAttentionReason.STATUS_REVIEW);
            }
            return;
        }
        int targetQuantity = order.targetInventoryQuantity(canceled ? 0 : order.getRemainQuantity());
        if (changeAppliedQuantity(order, targetQuantity) && order.pendingReturnQuantity() > 0) {
            order.requireAttention(SmartStoreOrderAttentionReason.RETURN_REVIEW);
        }
    }

    private boolean ensureMapping(SmartStoreProductOrder order) {
        if (order.hasMapping()) {
            return true;
        }
        Optional<SmartStoreOrderMappingHistory> previous = orderMappingHistoryPort.findResolvable(
                order.getOriginProductNo(), order.getItemNo(), orderReferenceAt(order));
        if (previous.isPresent()) {
            SmartStoreOrderMappingHistory found = previous.get();
            order.mapTo(found.getProductId(), found.getProductVariantId());
            return true;
        }
        Optional<SmartStoreStockMapping> mapping = order.getItemNo() == null
                ? mappingPort.findByOriginProductNoAndProductVariantIdIsNull(order.getOriginProductNo())
                : mappingPort.findByOriginProductNoAndOptionId(order.getOriginProductNo(), order.getItemNo());
        if (mapping.isEmpty() || !mapping.get().isEnabled()) {
            order.requireAttention(SmartStoreOrderAttentionReason.MAPPING_REQUIRED);
            return false;
        }
        SmartStoreStockMapping found = mapping.get();
        order.mapTo(found.getProductId(), found.getProductVariantId());
        return true;
    }

    private static LocalDateTime orderReferenceAt(SmartStoreProductOrder order) {
        return order.getPaymentDate() == null ? order.getLastChangedAt() : order.getPaymentDate();
    }

    private boolean changeAppliedQuantity(SmartStoreProductOrder order, int targetQuantity) {
        int currentQuantity = order.getInventoryAppliedQuantity();
        if (targetQuantity == currentQuantity) {
            order.resolveAttention();
            return true;
        }
        if (!ensureMapping(order)) {
            return false;
        }
        if (targetQuantity > currentQuantity) {
            if (!tryDeduct(order, targetQuantity - currentQuantity)) {
                order.requireAttention(SmartStoreOrderAttentionReason.STOCK_SHORTAGE);
                return false;
            }
        } else {
            restore(order, currentQuantity - targetQuantity);
        }
        order.applyInventoryQuantity(targetQuantity);
        return true;
    }

    private boolean tryDeduct(SmartStoreProductOrder order, int quantity) {
        if (order.getProductVariantId() == null) {
            return inventoryService.tryDeduct(order.getProductId(), quantity);
        }
        return variantStockService.tryDeductCommittedSale(order.getProductVariantId(), quantity);
    }

    private void restore(SmartStoreProductOrder order, int quantity) {
        if (order.getProductVariantId() == null) {
            inventoryService.restore(order.getProductId(), quantity);
            return;
        }
        variantStockService.restoreAll(List.of(new VariantAdjustment(
                order.getProductVariantId(), quantity)));
    }

    private SmartStoreProductOrder lockedOrder(String productOrderId) {
        return orderPort.findByProductOrderIdWithLock(productOrderId)
                .orElseThrow(() -> new NotFoundException("스마트스토어 상품 주문"));
    }
}
