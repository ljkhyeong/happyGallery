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
    @Transactional(propagation = Propagation.NEVER)
    public ChannelOrderDetailResult detail(String productOrderId) {
        SmartStoreProductOrder order = order(productOrderId);
        SmartStoreOrderProvider.DeliveryInfo protectedInfo =
                deliveryInfoProtector.decrypt(order.getDeliveryInfoEnc());
        DeliveryInfo deliveryInfo = protectedInfo == null ? null : new DeliveryInfo(
                protectedInfo.recipientName(), protectedInfo.phone(), protectedInfo.postalCode(),
                protectedInfo.addressLine1(), protectedInfo.addressLine2(),
                protectedInfo.shippingMemo());
        ClaimDetail claimDetail = currentClaimDetail(productOrderId);
        return new ChannelOrderDetailResult(
                result(order), deliveryInfo, order.getPlaceOrderStatus(), order.getShippingDueDate(),
                order.getExpectedDeliveryMethod(), order.getDeliveryCompany(),
                order.getTrackingNumber(), order.getUnitPrice(), order.getPaymentAmount(),
                order.getPaymentCommission(), order.getSaleCommission(),
                order.getChannelCommission(), order.getExpectedSettlementAmount(), claimDetail);
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
    public void holdReturn(ReturnHoldCommand command) {
        requireEnabled();
        orderProvider.holdReturn(new SmartStoreOrderProvider.ReturnHoldCommand(
                command.productOrderId(), command.holdbackClassType(), command.detailedReason(),
                command.extraReturnFeeAmount()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void releaseReturnHold(String productOrderId) {
        requireEnabled();
        orderProvider.releaseReturnHold(productOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void requestSellerReturn(SellerReturnCommand command) {
        requireEnabled();
        orderProvider.requestSellerReturn(new SmartStoreOrderProvider.SellerReturnCommand(
                command.productOrderId(), command.returnReason(), command.collectDeliveryMethod(),
                command.collectDeliveryCompany(), command.collectTrackingNumber(),
                command.returnQuantity()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void dispatchExchange(ExchangeDispatchCommand command) {
        requireEnabled();
        orderProvider.dispatchExchange(new SmartStoreOrderProvider.ExchangeDispatchCommand(
                command.productOrderId(), command.deliveryMethod(), command.deliveryCompanyCode(),
                command.trackingNumber()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void completeExchangeCollect(String productOrderId) {
        requireEnabled();
        orderProvider.completeExchangeCollect(productOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void rejectExchange(ExchangeRejectCommand command) {
        requireEnabled();
        orderProvider.rejectExchange(new SmartStoreOrderProvider.ExchangeRejectCommand(
                command.productOrderId(), command.reason()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void holdExchange(ExchangeHoldCommand command) {
        requireEnabled();
        orderProvider.holdExchange(new SmartStoreOrderProvider.ExchangeHoldCommand(
                command.productOrderId(), command.holdbackClassType(), command.detailedReason(),
                command.extraExchangeFeeAmount()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void releaseExchangeHold(String productOrderId) {
        requireEnabled();
        orderProvider.releaseExchangeHold(productOrderId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void requestSellerCancel(SellerCancelCommand command) {
        requireEnabled();
        orderProvider.requestSellerCancel(new SmartStoreOrderProvider.SellerCancelCommand(
                command.productOrderId(), command.reason(), command.detailedReason(),
                command.quantity()));
    }

    private ClaimDetail currentClaimDetail(String productOrderId) {
        if (!orderProvider.isEnabled()) {
            return null;
        }
        SmartStoreOrderProvider.ClaimDetail claim = orderProvider.fetchDetails(List.of(productOrderId))
                .stream()
                .filter(detail -> productOrderId.equals(detail.productOrderId()))
                .map(SmartStoreOrderProvider.ProductOrderDetail::claimDetail)
                .findFirst()
                .orElse(null);
        return claim == null ? null : new ClaimDetail(
                claim.claimId(), claim.claimType(), claim.claimStatus(), claim.reason(),
                claim.detailedReason(), claim.requestQuantity(), claim.requestedAt(),
                claim.collectStatus(), claim.collectDeliveryCompany(),
                claim.collectTrackingNumber(), claim.claimDeliveryFeeDemandAmount(),
                claim.holdbackStatus(), claim.imageUrls());
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
