package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultSmartStoreChannelOrderService implements SmartStoreChannelOrderUseCase {

    private final SmartStoreProductOrderPort orderPort;
    private final SmartStoreOrderTransactionService transactionService;
    private final SmartStoreOrderProvider orderProvider;
    private final SmartStoreDeliveryInfoProtector deliveryInfoProtector;

    public DefaultSmartStoreChannelOrderService(
            SmartStoreProductOrderPort orderPort,
            SmartStoreOrderTransactionService transactionService,
            SmartStoreOrderProvider orderProvider,
            SmartStoreDeliveryInfoProtector deliveryInfoProtector) {
        this.orderPort = orderPort;
        this.transactionService = transactionService;
        this.orderProvider = orderProvider;
        this.deliveryInfoProtector = deliveryInfoProtector;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelOrderResult> list(boolean attentionOnly, int limit) {
        return orderPort.findRecent(attentionOnly, limit).stream().map(this::result).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelOrderDetailResult detail(String productOrderId) {
        SmartStoreProductOrder order = order(productOrderId);
        SmartStoreOrderProvider.DeliveryInfo protectedInfo =
                deliveryInfoProtector.decrypt(order.getDeliveryInfoEnc());
        DeliveryInfo deliveryInfo = protectedInfo == null ? null : new DeliveryInfo(
                protectedInfo.recipientName(), protectedInfo.phone(), protectedInfo.postalCode(),
                protectedInfo.addressLine1(), protectedInfo.addressLine2(),
                protectedInfo.shippingMemo());
        return new ChannelOrderDetailResult(
                result(order), deliveryInfo, order.getPlaceOrderStatus(), order.getShippingDueDate(),
                order.getExpectedDeliveryMethod(), order.getDeliveryCompany(),
                order.getTrackingNumber(), order.getUnitPrice(), order.getPaymentAmount(),
                order.getPaymentCommission(), order.getSaleCommission(),
                order.getChannelCommission(), order.getExpectedSettlementAmount());
    }

    @Override
    public ChannelOrderResult retryInventory(String productOrderId) {
        return result(transactionService.retryInventory(productOrderId));
    }

    @Override
    public ChannelOrderResult resolveReturn(String productOrderId, boolean restoreStock) {
        return result(transactionService.resolveReturn(productOrderId, restoreStock));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void confirm(String productOrderId) {
        requireEnabled();
        orderProvider.confirm(productOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void dispatch(DispatchCommand command) {
        requireEnabled();
        orderProvider.dispatch(new SmartStoreOrderProvider.DispatchCommand(
                command.productOrderId(), command.deliveryMethod(), command.deliveryCompanyCode(),
                command.trackingNumber(), command.dispatchDate()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void delay(DelayCommand command) {
        requireEnabled();
        orderProvider.delay(new SmartStoreOrderProvider.DelayCommand(
                command.productOrderId(), command.dispatchDueDate(), command.reasonCode(),
                command.detailedReason()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void approveCancel(String productOrderId) {
        requireEnabled();
        orderProvider.approveCancel(productOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void approveReturn(String productOrderId) {
        requireEnabled();
        orderProvider.approveReturn(productOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void rejectReturn(String productOrderId) {
        requireEnabled();
        orderProvider.rejectReturn(productOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void dispatchExchange(ExchangeDispatchCommand command) {
        requireEnabled();
        orderProvider.dispatchExchange(new SmartStoreOrderProvider.ExchangeDispatchCommand(
                command.productOrderId(), command.deliveryMethod(), command.deliveryCompanyCode(),
                command.trackingNumber()));
    }

    private SmartStoreProductOrder order(String productOrderId) {
        return orderPort.findByProductOrderId(productOrderId)
                .orElseThrow(() -> new NotFoundException("스마트스토어 상품 주문"));
    }

    private void requireEnabled() {
        if (!orderProvider.isEnabled()) {
            throw conflict("스마트스토어 연동이 비활성화되어 있습니다.");
        }
    }

    private static HappyGalleryException conflict(String message) {
        return new HappyGalleryException(ErrorCode.CONFLICT, message);
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
