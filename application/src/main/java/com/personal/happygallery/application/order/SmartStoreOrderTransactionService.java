package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderChange;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.ProductVariantStockService;
import com.personal.happygallery.application.product.ProductVariantStockService.VariantAdjustment;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import java.util.List;
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
    private final InventoryService inventoryService;
    private final ProductVariantStockService variantStockService;
    private final SmartStoreDeliveryInfoProtector deliveryInfoProtector;

    SmartStoreOrderTransactionService(
            SmartStoreProductOrderPort orderPort,
            SmartStoreStockMappingPort mappingPort,
            InventoryService inventoryService,
            ProductVariantStockService variantStockService,
            SmartStoreDeliveryInfoProtector deliveryInfoProtector) {
        this.orderPort = orderPort;
        this.mappingPort = mappingPort;
        this.inventoryService = inventoryService;
        this.variantStockService = variantStockService;
        this.deliveryInfoProtector = deliveryInfoProtector;
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
    public SmartStoreProductOrder resolveReturn(String productOrderId, boolean restoreStock) {
        SmartStoreProductOrder order = lockedOrder(productOrderId);
        int restoreQuantity = order.resolveReturn(restoreStock);
        if (restoreQuantity > 0) {
            restore(order, restoreQuantity);
        }
        return orderPort.save(order);
    }

    private void reconcile(SmartStoreProductOrder order) {
        String status = order.getProductOrderStatus();
        boolean canceled = CANCELED_STATUSES.contains(status) || "PAYMENT_WAITING".equals(status);
        if (!canceled && !"RETURNED".equals(status) && !ACTIVE_STOCK_STATUSES.contains(status)) {
            order.requireAttention(SmartStoreOrderAttentionReason.STATUS_REVIEW);
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
        return variantStockService.tryDeduct(order.getProductVariantId(), quantity);
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
