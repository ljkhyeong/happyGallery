package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.OperationNotSentException;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.OperationRejectedException;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.OperationResultUnknownException;
import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultSmartStoreChannelOrderService implements SmartStoreChannelOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultSmartStoreChannelOrderService.class);

    private final SmartStoreProductOrderPort orderPort;
    private final SmartStoreOrderTransactionService transactionService;
    private final SmartStoreOrderProvider orderProvider;
    private final SmartStoreDeliveryInfoProtector deliveryInfoProtector;
    private final SmartStoreOrderActionHistoryService actionHistoryService;

    public DefaultSmartStoreChannelOrderService(
            SmartStoreProductOrderPort orderPort,
            SmartStoreOrderTransactionService transactionService,
            SmartStoreOrderProvider orderProvider,
            SmartStoreDeliveryInfoProtector deliveryInfoProtector,
            SmartStoreOrderActionHistoryService actionHistoryService) {
        this.orderPort = orderPort;
        this.transactionService = transactionService;
        this.orderProvider = orderProvider;
        this.deliveryInfoProtector = deliveryInfoProtector;
        this.actionHistoryService = actionHistoryService;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ChannelOrderResult> list(
            boolean attentionOnly,
            SmartStoreOrderAttentionReason attentionReason,
            String cursor,
            int size) {
        int pageSize = PageParams.requireSize(size);
        SmartStoreOrderCursor.CursorParam cursorParam = cursor == null
                ? null : SmartStoreOrderCursor.decode(cursor);
        List<SmartStoreProductOrder> orders = orderPort.findRecentPage(
                attentionOnly,
                attentionReason,
                cursorParam == null ? null : cursorParam.changedAt(),
                cursorParam == null ? null : cursorParam.productOrderId(),
                pageSize + 1);
        List<ChannelOrderResult> results = orders.stream().map(this::result).toList();
        return CursorPage.of(results, pageSize, item -> SmartStoreOrderCursor.encode(
                item.lastChangedAt(), item.productOrderId()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public List<ReturnDeliveryCompanyResult> listReturnDeliveryCompanies() {
        requireEnabled();
        return orderProvider.findReturnDeliveryCompanies().stream()
                .map(company -> new ReturnDeliveryCompanyResult(
                        company.id(), company.name(), company.returnDeliveryCompanyPriorityType()))
                .toList();
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
    public ChannelOrderResult resolveReturn(String productOrderId, boolean restoreStock, String reviewVersion) {
        return result(transactionService.resolveReturn(productOrderId, restoreStock, reviewVersion));
    }

    @Override
    public ChannelOrderResult resolveInventory(InventoryResolutionCommand command, AdminActor actor) {
        return result(transactionService.resolveInventory(command, actor));
    }

    @Override
    public List<ActionHistoryResult> listActionHistory(String productOrderId) {
        order(productOrderId);
        return actionHistoryService.list(productOrderId);
    }

    @Override
    public CursorPage<ActionHistoryResult> listUnresolvedActions(String cursor, int size) {
        return actionHistoryService.listUnresolved(cursor, size);
    }

    @Override
    public ActionHistoryResult reconcileAction(
            long historyId,
            ReconcileActionCommand command,
            AdminActor actor) {
        return actionHistoryService.reconcile(historyId, command.outcome(), command.note(), actor);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public CurrentOrderStatusResult currentStatus(String productOrderId) {
        order(productOrderId);
        requireEnabled();
        SmartStoreOrderProvider.ProductOrderDetail detail = orderProvider.fetchDetails(List.of(productOrderId))
                .stream()
                .filter(candidate -> productOrderId.equals(candidate.productOrderId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("네이버 스마트스토어 상품 주문"));
        return new CurrentOrderStatusResult(
                detail.productOrderId(), detail.productOrderStatus(), detail.placeOrderStatus(),
                detail.claimType(), detail.claimStatus(), detail.remainQuantity(),
                detail.shippingDueDate(), detail.expectedDeliveryMethod(), detail.deliveryCompany(),
                detail.trackingNumber(), claimDetail(detail.claimDetail()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void confirm(String productOrderId, AdminActor actor) {
        requireEnabled();
        executeAudited(
                productOrderId, SmartStoreOrderAction.ORDER_CONFIRMED, null, actor,
                () -> orderProvider.confirm(productOrderId));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public BulkOperationResult confirmAll(List<String> productOrderIds, AdminActor actor) {
        requireEnabled();
        return executeBulkAudited(
                productOrderIds,
                SmartStoreOrderAction.ORDER_CONFIRMED,
                ignored -> null,
                actor,
                () -> orderProvider.confirmAll(productOrderIds));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void dispatch(DispatchCommand command, AdminActor actor) {
        requireEnabled();
        executeAudited(
                command.productOrderId(),
                SmartStoreOrderAction.ORDER_DISPATCHED,
                dispatchSummary(command),
                actor,
                () -> orderProvider.dispatch(providerCommand(command)));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public BulkOperationResult dispatchAll(List<DispatchCommand> commands, AdminActor actor) {
        requireEnabled();
        Map<String, DispatchCommand> commandsById = commands.stream()
                .collect(Collectors.toMap(
                        DispatchCommand::productOrderId,
                        Function.identity(),
                        (first, ignored) -> first));
        return executeBulkAudited(
                commands.stream().map(DispatchCommand::productOrderId).toList(),
                SmartStoreOrderAction.ORDER_DISPATCHED,
                productOrderId -> dispatchSummary(commandsById.get(productOrderId)),
                actor,
                () -> orderProvider.dispatchAll(commands.stream()
                        .map(DefaultSmartStoreChannelOrderService::providerCommand)
                        .toList()));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void delay(DelayCommand command, AdminActor actor) {
        requireEnabled();
        executeAudited(
                command.productOrderId(), SmartStoreOrderAction.ORDER_DELAYED,
                "발송 기한 %s, 사유 코드 %s, 상세 사유 %s".formatted(
                        command.dispatchDueDate(), command.reasonCode(), command.detailedReason()),
                actor,
                () -> orderProvider.delay(new SmartStoreOrderProvider.DelayCommand(
                        command.productOrderId(), command.dispatchDueDate(), command.reasonCode(),
                        command.detailedReason())));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void approveCancel(String productOrderId, AdminActor actor) {
        executeEnabledAction(
                productOrderId, SmartStoreOrderAction.CANCEL_APPROVED, null, actor,
                () -> orderProvider.approveCancel(productOrderId));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void approveReturn(String productOrderId, AdminActor actor) {
        executeEnabledAction(
                productOrderId, SmartStoreOrderAction.RETURN_APPROVED, null, actor,
                () -> orderProvider.approveReturn(productOrderId));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void rejectReturn(String productOrderId, AdminActor actor) {
        executeEnabledAction(
                productOrderId, SmartStoreOrderAction.RETURN_REJECTED, null, actor,
                () -> orderProvider.rejectReturn(productOrderId));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void holdReturn(ReturnHoldCommand command, AdminActor actor) {
        executeEnabledAction(
                command.productOrderId(), SmartStoreOrderAction.RETURN_HELD,
                "보류 사유 %s, 상세 사유 %s, 추가 반품비 %s".formatted(
                        command.holdbackClassType(), command.detailedReason(),
                        command.extraReturnFeeAmount()),
                actor,
                () -> orderProvider.holdReturn(new SmartStoreOrderProvider.ReturnHoldCommand(
                        command.productOrderId(), command.holdbackClassType(), command.detailedReason(),
                        command.extraReturnFeeAmount())));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void releaseReturnHold(String productOrderId, AdminActor actor) {
        executeEnabledAction(
                productOrderId, SmartStoreOrderAction.RETURN_HOLD_RELEASED, null, actor,
                () -> orderProvider.releaseReturnHold(productOrderId));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void requestSellerReturn(SellerReturnCommand command, AdminActor actor) {
        executeEnabledAction(
                command.productOrderId(), SmartStoreOrderAction.RETURN_REQUESTED,
                "반품 사유 %s, 수거 방법 %s, 수거 택배사 %s, 운송장 %s, 수량 %s".formatted(
                        command.returnReason(), command.collectDeliveryMethod(),
                        command.collectDeliveryCompany(), command.collectTrackingNumber(),
                        command.returnQuantity()),
                actor,
                () -> orderProvider.requestSellerReturn(new SmartStoreOrderProvider.SellerReturnCommand(
                        command.productOrderId(), command.returnReason(), command.collectDeliveryMethod(),
                        command.collectDeliveryCompany(), command.collectTrackingNumber(),
                        command.returnQuantity())));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void dispatchExchange(ExchangeDispatchCommand command, AdminActor actor) {
        executeEnabledAction(
                command.productOrderId(), SmartStoreOrderAction.EXCHANGE_DISPATCHED,
                "배송 방법 %s, 택배사 %s, 운송장 %s".formatted(
                        command.deliveryMethod(), command.deliveryCompanyCode(), command.trackingNumber()),
                actor,
                () -> orderProvider.dispatchExchange(new SmartStoreOrderProvider.ExchangeDispatchCommand(
                        command.productOrderId(), command.deliveryMethod(), command.deliveryCompanyCode(),
                        command.trackingNumber())));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void completeExchangeCollect(String productOrderId, AdminActor actor) {
        executeEnabledAction(
                productOrderId, SmartStoreOrderAction.EXCHANGE_COLLECTION_COMPLETED, null, actor,
                () -> orderProvider.completeExchangeCollect(productOrderId));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void rejectExchange(ExchangeRejectCommand command, AdminActor actor) {
        executeEnabledAction(
                command.productOrderId(), SmartStoreOrderAction.EXCHANGE_REJECTED,
                "거절 사유 %s".formatted(command.reason()), actor,
                () -> orderProvider.rejectExchange(new SmartStoreOrderProvider.ExchangeRejectCommand(
                        command.productOrderId(), command.reason())));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void holdExchange(ExchangeHoldCommand command, AdminActor actor) {
        executeEnabledAction(
                command.productOrderId(), SmartStoreOrderAction.EXCHANGE_HELD,
                "보류 사유 %s, 상세 사유 %s, 추가 교환비 %s".formatted(
                        command.holdbackClassType(), command.detailedReason(),
                        command.extraExchangeFeeAmount()),
                actor,
                () -> orderProvider.holdExchange(new SmartStoreOrderProvider.ExchangeHoldCommand(
                        command.productOrderId(), command.holdbackClassType(), command.detailedReason(),
                        command.extraExchangeFeeAmount())));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void releaseExchangeHold(String productOrderId, AdminActor actor) {
        executeEnabledAction(
                productOrderId, SmartStoreOrderAction.EXCHANGE_HOLD_RELEASED, null, actor,
                () -> orderProvider.releaseExchangeHold(productOrderId));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void requestSellerCancel(SellerCancelCommand command, AdminActor actor) {
        executeEnabledAction(
                command.productOrderId(), SmartStoreOrderAction.CANCEL_REQUESTED,
                "취소 사유 %s, 상세 사유 %s, 수량 %s".formatted(
                        command.reason(), command.detailedReason(), command.quantity()),
                actor,
                () -> orderProvider.requestSellerCancel(new SmartStoreOrderProvider.SellerCancelCommand(
                        command.productOrderId(), command.reason(), command.detailedReason(),
                        command.quantity())));
    }

    private void executeEnabledAction(
            String productOrderId,
            SmartStoreOrderAction action,
            String requestSummary,
            AdminActor actor,
            Runnable operation) {
        requireEnabled();
        executeAudited(productOrderId, action, requestSummary, actor, operation);
    }

    private void executeAudited(
            String productOrderId,
            SmartStoreOrderAction action,
            String requestSummary,
            AdminActor actor,
            Runnable operation) {
        long historyId = actionHistoryService.start(productOrderId, action, requestSummary, actor);
        try {
            operation.run();
            completeAudit(() -> actionHistoryService.succeed(historyId), historyId);
        } catch (OperationNotSentException exception) {
            completeAudit(
                    () -> actionHistoryService.markNotSent(
                            historyId, exception.code(), exception.getMessage()),
                    historyId);
            throw exception;
        } catch (OperationRejectedException exception) {
            completeAudit(
                    () -> actionHistoryService.reject(historyId, exception.code(), exception.getMessage()),
                    historyId);
            throw exception;
        } catch (OperationResultUnknownException exception) {
            completeAudit(
                    () -> actionHistoryService.markResultUnknown(historyId, exception.getMessage()),
                    historyId);
            throw exception;
        } catch (RuntimeException exception) {
            completeAudit(
                    () -> actionHistoryService.markResultUnknown(
                            historyId, "예상하지 못한 오류로 처리 결과를 확인할 수 없습니다."),
                    historyId);
            throw exception;
        }
    }

    private BulkOperationResult executeBulkAudited(
            List<String> productOrderIds,
            SmartStoreOrderAction action,
            Function<String, String> summary,
            AdminActor actor,
            Operation operation) {
        List<StartedAction> started = productOrderIds.stream()
                .map(productOrderId -> new StartedAction(
                        productOrderId,
                        actionHistoryService.start(productOrderId, action, summary.apply(productOrderId), actor)))
                .toList();
        SmartStoreOrderProvider.OperationResult result;
        try {
            result = operation.execute();
        } catch (OperationNotSentException exception) {
            started.forEach(item -> completeAudit(
                    () -> actionHistoryService.markNotSent(
                            item.historyId(), exception.code(), exception.getMessage()),
                    item.historyId()));
            throw exception;
        } catch (OperationRejectedException exception) {
            started.forEach(item -> completeAudit(
                    () -> actionHistoryService.reject(
                            item.historyId(), exception.code(), exception.getMessage()),
                    item.historyId()));
            throw exception;
        } catch (OperationResultUnknownException exception) {
            started.forEach(item -> completeAudit(
                    () -> actionHistoryService.markResultUnknown(
                            item.historyId(), exception.getMessage()),
                    item.historyId()));
            throw exception;
        } catch (RuntimeException exception) {
            started.forEach(item -> completeAudit(
                    () -> actionHistoryService.markResultUnknown(
                            item.historyId(), "예상하지 못한 오류로 처리 결과를 확인할 수 없습니다."),
                    item.historyId()));
            throw exception;
        }

        Map<String, SmartStoreOrderProvider.OperationFailure> failures = result.failures().stream()
                .collect(Collectors.toMap(
                        SmartStoreOrderProvider.OperationFailure::productOrderId,
                        Function.identity(),
                        (first, ignored) -> first));
        started.forEach(item -> {
            SmartStoreOrderProvider.OperationFailure failure = failures.get(item.productOrderId());
            if (failure == null) {
                completeAudit(() -> actionHistoryService.succeed(item.historyId()), item.historyId());
            } else if ("UNKNOWN_RESULT".equals(failure.code())) {
                completeAudit(
                        () -> actionHistoryService.markResultUnknown(item.historyId(), failure.message()),
                        item.historyId());
            } else {
                completeAudit(
                        () -> actionHistoryService.reject(
                                item.historyId(), failure.code(), failure.message()),
                        item.historyId());
            }
        });
        return bulkResult(result);
    }

    private static SmartStoreOrderProvider.DispatchCommand providerCommand(DispatchCommand command) {
        return new SmartStoreOrderProvider.DispatchCommand(
                command.productOrderId(), command.deliveryMethod(), command.deliveryCompanyCode(),
                command.trackingNumber(), command.dispatchDate());
    }

    private static String dispatchSummary(DispatchCommand command) {
        return "배송 방법 %s, 택배사 %s, 운송장 %s, 발송일 %s".formatted(
                command.deliveryMethod(), command.deliveryCompanyCode(),
                command.trackingNumber(), command.dispatchDate());
    }

    private static void completeAudit(Runnable completion, long historyId) {
        try {
            completion.run();
        } catch (RuntimeException exception) {
            log.error("스마트스토어 주문 처리 이력 완료 저장 실패: historyId={}", historyId, exception);
        }
    }

    private ClaimDetail currentClaimDetail(String productOrderId) {
        if (!orderProvider.isEnabled()) {
            return null;
        }
        SmartStoreOrderProvider.ClaimDetail claim = orderProvider.fetchDetails(List.of(productOrderId))
                .stream()
                .filter(detail -> productOrderId.equals(detail.productOrderId()))
                .findFirst()
                .map(SmartStoreOrderProvider.ProductOrderDetail::claimDetail)
                .orElse(null);
        return claimDetail(claim);
    }

    private static ClaimDetail claimDetail(SmartStoreOrderProvider.ClaimDetail claim) {
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

    private static BulkOperationResult bulkResult(SmartStoreOrderProvider.OperationResult result) {
        return new BulkOperationResult(
                result.successProductOrderIds(),
                result.failures().stream()
                        .map(failure -> new BulkOperationFailure(
                                failure.productOrderId(), failure.code(), failure.message()))
                        .toList());
    }

    private ChannelOrderResult result(SmartStoreProductOrder order) {
        return new ChannelOrderResult(
                order.getProductOrderId(), order.getOrderId(), order.getOriginProductNo(),
                order.getItemNo(), order.getProductId(), order.getProductVariantId(),
                order.getProductName(), order.getProductOption(), order.getProductOrderStatus(),
                order.getClaimType(), order.getClaimStatus(), order.getInitialQuantity(),
                order.getRemainQuantity(), order.getInventoryAppliedQuantity(),
                order.getAttentionReason(), order.getPaymentDate(), order.getLastChangedAt(),
                order.pendingReturnQuantity(), order.returnReviewVersion(),
                order.inventoryResolutionVersion());
    }

    @FunctionalInterface
    private interface Operation {
        SmartStoreOrderProvider.OperationResult execute();
    }

    private record StartedAction(String productOrderId, long historyId) {}
}
