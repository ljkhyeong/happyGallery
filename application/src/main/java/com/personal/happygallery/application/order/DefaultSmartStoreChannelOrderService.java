package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultSmartStoreChannelOrderService implements SmartStoreChannelOrderUseCase {

    private final SmartStoreProductOrderPort orderPort;
    private final SmartStoreOrderTransactionService transactionService;

    public DefaultSmartStoreChannelOrderService(
            SmartStoreProductOrderPort orderPort,
            SmartStoreOrderTransactionService transactionService) {
        this.orderPort = orderPort;
        this.transactionService = transactionService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelOrderResult> list(boolean attentionOnly, int limit) {
        return orderPort.findRecent(attentionOnly, limit).stream().map(this::result).toList();
    }

    @Override
    public ChannelOrderResult retryInventory(String productOrderId) {
        return result(transactionService.retryInventory(productOrderId));
    }

    @Override
    public ChannelOrderResult resolveReturn(String productOrderId, boolean restoreStock) {
        return result(transactionService.resolveReturn(productOrderId, restoreStock));
    }

    private ChannelOrderResult result(SmartStoreProductOrder order) {
        return new ChannelOrderResult(
                order.getProductOrderId(), order.getOrderId(), order.getOriginProductNo(),
                order.getItemNo(), order.getProductId(), order.getProductVariantId(),
                order.getProductName(), order.getProductOption(), order.getProductOrderStatus(),
                order.getClaimType(), order.getClaimStatus(), order.getInitialQuantity(),
                order.getRemainQuantity(), order.getInventoryAppliedQuantity(),
                order.getAttentionReason(), order.getPaymentDate(), order.getLastChangedAt());
    }
}
