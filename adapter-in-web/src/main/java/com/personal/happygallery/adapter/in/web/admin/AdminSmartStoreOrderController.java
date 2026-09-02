package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.DelaySmartStoreOrderRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.BulkConfirmSmartStoreOrdersRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.BulkDispatchSmartStoreOrdersRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.DispatchSmartStoreExchangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.DispatchSmartStoreOrderRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.HoldSmartStoreExchangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.HoldSmartStoreReturnRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.RejectSmartStoreExchangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.ReconcileSmartStoreOrderActionRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.RequestSmartStoreSellerCancelRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.RequestSmartStoreSellerReturnRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.ResolveSmartStoreInventoryRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.ResolveSmartStoreReturnRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreChannelOrderDetailResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreChannelOrderPageResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreChannelOrderResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreOrderBulkActionResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreOrderActionHistoryResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreOrderActionPageResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreCurrentOrderStatusResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreReturnDeliveryCompanyResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.AdminActor;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/smartstore-orders")
public class AdminSmartStoreOrderController {

    private final SmartStoreChannelOrderUseCase channelOrderUseCase;

    public AdminSmartStoreOrderController(SmartStoreChannelOrderUseCase channelOrderUseCase) {
        this.channelOrderUseCase = channelOrderUseCase;
    }

    @GetMapping
    @Operation(operationId = "listSmartStoreChannelOrders")
    public SmartStoreChannelOrderPageResponse list(
            @RequestParam(defaultValue = "false") boolean attentionOnly,
            @RequestParam(required = false) SmartStoreOrderAttentionReason attentionReason,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return SmartStoreChannelOrderPageResponse.from(
                channelOrderUseCase.list(attentionOnly, attentionReason, cursor, size));
    }

    @GetMapping("/return-delivery-companies")
    @Operation(operationId = "listSmartStoreReturnDeliveryCompanies")
    public List<SmartStoreReturnDeliveryCompanyResponse> listReturnDeliveryCompanies() {
        return channelOrderUseCase.listReturnDeliveryCompanies().stream()
                .map(SmartStoreReturnDeliveryCompanyResponse::from).toList();
    }

    @GetMapping("/actions/unresolved")
    @Operation(operationId = "listUnresolvedSmartStoreOrderActions")
    public SmartStoreOrderActionPageResponse listUnresolvedActions(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return SmartStoreOrderActionPageResponse.from(
                channelOrderUseCase.listUnresolvedActions(cursor, size));
    }

    @PostMapping("/actions/{historyId}/reconciliation")
    @Operation(operationId = "reconcileSmartStoreOrderAction")
    public SmartStoreOrderActionHistoryResponse reconcileAction(
            @PathVariable @Positive long historyId,
            @Valid @RequestBody ReconcileSmartStoreOrderActionRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return SmartStoreOrderActionHistoryResponse.from(
                channelOrderUseCase.reconcileAction(historyId, request.toCommand(), actor(admin)));
    }

    @GetMapping("/{productOrderId}")
    @Operation(operationId = "getSmartStoreChannelOrder")
    public SmartStoreChannelOrderDetailResponse detail(@PathVariable String productOrderId) {
        return SmartStoreChannelOrderDetailResponse.from(channelOrderUseCase.detail(productOrderId));
    }

    @GetMapping("/{productOrderId}/current-status")
    @Operation(operationId = "getCurrentSmartStoreOrderStatus")
    public SmartStoreCurrentOrderStatusResponse currentStatus(@PathVariable String productOrderId) {
        return SmartStoreCurrentOrderStatusResponse.from(
                channelOrderUseCase.currentStatus(productOrderId));
    }

    @PostMapping("/{productOrderId}/inventory/retry")
    @Operation(operationId = "retrySmartStoreChannelOrderInventory")
    public SmartStoreChannelOrderResponse retryInventory(@PathVariable String productOrderId) {
        return SmartStoreChannelOrderResponse.from(
                channelOrderUseCase.retryInventory(productOrderId));
    }

    @PostMapping("/{productOrderId}/inventory-resolution")
    @Operation(operationId = "resolveSmartStoreChannelOrderInventory")
    public SmartStoreChannelOrderResponse resolveInventory(
            @PathVariable String productOrderId,
            @Valid @RequestBody ResolveSmartStoreInventoryRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return SmartStoreChannelOrderResponse.from(
                channelOrderUseCase.resolveInventory(request.toCommand(productOrderId), actor(admin)));
    }

    @GetMapping("/{productOrderId}/actions")
    @Operation(operationId = "listSmartStoreChannelOrderActions")
    public List<SmartStoreOrderActionHistoryResponse> listActions(
            @PathVariable String productOrderId) {
        return channelOrderUseCase.listActionHistory(productOrderId).stream()
                .map(SmartStoreOrderActionHistoryResponse::from)
                .toList();
    }

    @PostMapping("/{productOrderId}/return-resolution")
    @Operation(operationId = "resolveSmartStoreChannelOrderReturn")
    public SmartStoreChannelOrderResponse resolveReturn(
            @PathVariable String productOrderId,
            @Valid @RequestBody ResolveSmartStoreReturnRequest request) {
        return SmartStoreChannelOrderResponse.from(
                channelOrderUseCase.resolveReturn(productOrderId, request.restoreStock(), request.reviewVersion()));
    }

    @PostMapping("/{productOrderId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "confirmSmartStoreChannelOrder")
    public void confirm(
            @PathVariable String productOrderId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.confirm(productOrderId, actor(admin));
    }

    @PostMapping("/{productOrderId}/dispatch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "dispatchSmartStoreChannelOrder")
    public void dispatch(
            @PathVariable String productOrderId,
            @Valid @RequestBody DispatchSmartStoreOrderRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.dispatch(request.toCommand(productOrderId), actor(admin));
    }

    @PostMapping("/confirm")
    @Operation(operationId = "confirmSmartStoreChannelOrders")
    public SmartStoreOrderBulkActionResponse confirmAll(
            @Valid @RequestBody BulkConfirmSmartStoreOrdersRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return SmartStoreOrderBulkActionResponse.from(
                channelOrderUseCase.confirmAll(request.productOrderIds(), actor(admin)));
    }

    @PostMapping("/dispatch")
    @Operation(operationId = "dispatchSmartStoreChannelOrders")
    public SmartStoreOrderBulkActionResponse dispatchAll(
            @Valid @RequestBody BulkDispatchSmartStoreOrdersRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return SmartStoreOrderBulkActionResponse.from(
                channelOrderUseCase.dispatchAll(request.toCommands(), actor(admin)));
    }

    @PostMapping("/{productOrderId}/delay")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "delaySmartStoreChannelOrder")
    public void delay(
            @PathVariable String productOrderId,
            @Valid @RequestBody DelaySmartStoreOrderRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.delay(request.toCommand(productOrderId), actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/cancel/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "approveSmartStoreCancelClaim")
    public void approveCancel(
            @PathVariable String productOrderId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.approveCancel(productOrderId, actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/return/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "approveSmartStoreReturnClaim")
    public void approveReturn(
            @PathVariable String productOrderId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.approveReturn(productOrderId, actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/return/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "rejectSmartStoreReturnClaim")
    public void rejectReturn(
            @PathVariable String productOrderId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.rejectReturn(productOrderId, actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/return/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "holdSmartStoreReturnClaim")
    public void holdReturn(
            @PathVariable String productOrderId,
            @Valid @RequestBody HoldSmartStoreReturnRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.holdReturn(request.toCommand(productOrderId), actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/return/hold/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "releaseSmartStoreReturnHold")
    public void releaseReturnHold(
            @PathVariable String productOrderId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.releaseReturnHold(productOrderId, actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/return/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "requestSmartStoreSellerReturn")
    public void requestSellerReturn(
            @PathVariable String productOrderId,
            @Valid @RequestBody RequestSmartStoreSellerReturnRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.requestSellerReturn(request.toCommand(productOrderId), actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/exchange/dispatch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "dispatchSmartStoreExchangeClaim")
    public void dispatchExchange(
            @PathVariable String productOrderId,
            @Valid @RequestBody DispatchSmartStoreExchangeRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.dispatchExchange(request.toCommand(productOrderId), actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/exchange/collect/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "completeSmartStoreExchangeCollect")
    public void completeExchangeCollect(
            @PathVariable String productOrderId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.completeExchangeCollect(productOrderId, actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/exchange/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "rejectSmartStoreExchangeClaim")
    public void rejectExchange(
            @PathVariable String productOrderId,
            @Valid @RequestBody RejectSmartStoreExchangeRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.rejectExchange(request.toCommand(productOrderId), actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/exchange/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "holdSmartStoreExchangeClaim")
    public void holdExchange(
            @PathVariable String productOrderId,
            @Valid @RequestBody HoldSmartStoreExchangeRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.holdExchange(request.toCommand(productOrderId), actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/exchange/hold/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "releaseSmartStoreExchangeHold")
    public void releaseExchangeHold(
            @PathVariable String productOrderId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.releaseExchangeHold(productOrderId, actor(admin));
    }

    @PostMapping("/{productOrderId}/claims/cancel/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "requestSmartStoreSellerCancel")
    public void requestSellerCancel(
            @PathVariable String productOrderId,
            @Valid @RequestBody RequestSmartStoreSellerCancelRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        channelOrderUseCase.requestSellerCancel(request.toCommand(productOrderId), actor(admin));
    }

    private static AdminActor actor(AdminPrincipal admin) {
        return new AdminActor(admin.auditActorId(), admin.getName());
    }
}
