package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.DelaySmartStoreOrderRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.BulkConfirmSmartStoreOrdersRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.BulkDispatchSmartStoreOrdersRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.DispatchSmartStoreExchangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.DispatchSmartStoreOrderRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.HoldSmartStoreExchangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.HoldSmartStoreReturnRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.RejectSmartStoreExchangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.RequestSmartStoreSellerCancelRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.RequestSmartStoreSellerReturnRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.ResolveSmartStoreReturnRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreChannelOrderDetailResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreChannelOrderResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreOrderBulkActionResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreReturnDeliveryCompanyResponse;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
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
    public List<SmartStoreChannelOrderResponse> list(
            @RequestParam(defaultValue = "false") boolean attentionOnly,
            @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit) {
        return channelOrderUseCase.list(attentionOnly, limit).stream()
                .map(SmartStoreChannelOrderResponse::from)
                .toList();
    }

    @GetMapping("/return-delivery-companies")
    @Operation(operationId = "listSmartStoreReturnDeliveryCompanies")
    public List<SmartStoreReturnDeliveryCompanyResponse> listReturnDeliveryCompanies() {
        return channelOrderUseCase.listReturnDeliveryCompanies().stream()
                .map(SmartStoreReturnDeliveryCompanyResponse::from).toList();
    }

    @GetMapping("/{productOrderId}")
    @Operation(operationId = "getSmartStoreChannelOrder")
    public SmartStoreChannelOrderDetailResponse detail(@PathVariable String productOrderId) {
        return SmartStoreChannelOrderDetailResponse.from(channelOrderUseCase.detail(productOrderId));
    }

    @PostMapping("/{productOrderId}/inventory/retry")
    @Operation(operationId = "retrySmartStoreChannelOrderInventory")
    public SmartStoreChannelOrderResponse retryInventory(@PathVariable String productOrderId) {
        return SmartStoreChannelOrderResponse.from(
                channelOrderUseCase.retryInventory(productOrderId));
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
    public void confirm(@PathVariable String productOrderId) {
        channelOrderUseCase.confirm(productOrderId);
    }

    @PostMapping("/{productOrderId}/dispatch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "dispatchSmartStoreChannelOrder")
    public void dispatch(
            @PathVariable String productOrderId,
            @Valid @RequestBody DispatchSmartStoreOrderRequest request) {
        channelOrderUseCase.dispatch(request.toCommand(productOrderId));
    }

    @PostMapping("/confirm")
    @Operation(operationId = "confirmSmartStoreChannelOrders")
    public SmartStoreOrderBulkActionResponse confirmAll(
            @Valid @RequestBody BulkConfirmSmartStoreOrdersRequest request) {
        return SmartStoreOrderBulkActionResponse.from(
                channelOrderUseCase.confirmAll(request.productOrderIds()));
    }

    @PostMapping("/dispatch")
    @Operation(operationId = "dispatchSmartStoreChannelOrders")
    public SmartStoreOrderBulkActionResponse dispatchAll(
            @Valid @RequestBody BulkDispatchSmartStoreOrdersRequest request) {
        return SmartStoreOrderBulkActionResponse.from(
                channelOrderUseCase.dispatchAll(request.toCommands()));
    }

    @PostMapping("/{productOrderId}/delay")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "delaySmartStoreChannelOrder")
    public void delay(
            @PathVariable String productOrderId,
            @Valid @RequestBody DelaySmartStoreOrderRequest request) {
        channelOrderUseCase.delay(request.toCommand(productOrderId));
    }

    @PostMapping("/{productOrderId}/claims/cancel/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "approveSmartStoreCancelClaim")
    public void approveCancel(@PathVariable String productOrderId) {
        channelOrderUseCase.approveCancel(productOrderId);
    }

    @PostMapping("/{productOrderId}/claims/return/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "approveSmartStoreReturnClaim")
    public void approveReturn(@PathVariable String productOrderId) {
        channelOrderUseCase.approveReturn(productOrderId);
    }

    @PostMapping("/{productOrderId}/claims/return/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "rejectSmartStoreReturnClaim")
    public void rejectReturn(@PathVariable String productOrderId) {
        channelOrderUseCase.rejectReturn(productOrderId);
    }

    @PostMapping("/{productOrderId}/claims/return/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "holdSmartStoreReturnClaim")
    public void holdReturn(
            @PathVariable String productOrderId,
            @Valid @RequestBody HoldSmartStoreReturnRequest request) {
        channelOrderUseCase.holdReturn(request.toCommand(productOrderId));
    }

    @PostMapping("/{productOrderId}/claims/return/hold/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "releaseSmartStoreReturnHold")
    public void releaseReturnHold(@PathVariable String productOrderId) {
        channelOrderUseCase.releaseReturnHold(productOrderId);
    }

    @PostMapping("/{productOrderId}/claims/return/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "requestSmartStoreSellerReturn")
    public void requestSellerReturn(
            @PathVariable String productOrderId,
            @Valid @RequestBody RequestSmartStoreSellerReturnRequest request) {
        channelOrderUseCase.requestSellerReturn(request.toCommand(productOrderId));
    }

    @PostMapping("/{productOrderId}/claims/exchange/dispatch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "dispatchSmartStoreExchangeClaim")
    public void dispatchExchange(
            @PathVariable String productOrderId,
            @Valid @RequestBody DispatchSmartStoreExchangeRequest request) {
        channelOrderUseCase.dispatchExchange(request.toCommand(productOrderId));
    }

    @PostMapping("/{productOrderId}/claims/exchange/collect/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "completeSmartStoreExchangeCollect")
    public void completeExchangeCollect(@PathVariable String productOrderId) {
        channelOrderUseCase.completeExchangeCollect(productOrderId);
    }

    @PostMapping("/{productOrderId}/claims/exchange/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "rejectSmartStoreExchangeClaim")
    public void rejectExchange(
            @PathVariable String productOrderId,
            @Valid @RequestBody RejectSmartStoreExchangeRequest request) {
        channelOrderUseCase.rejectExchange(request.toCommand(productOrderId));
    }

    @PostMapping("/{productOrderId}/claims/exchange/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "holdSmartStoreExchangeClaim")
    public void holdExchange(
            @PathVariable String productOrderId,
            @Valid @RequestBody HoldSmartStoreExchangeRequest request) {
        channelOrderUseCase.holdExchange(request.toCommand(productOrderId));
    }

    @PostMapping("/{productOrderId}/claims/exchange/hold/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "releaseSmartStoreExchangeHold")
    public void releaseExchangeHold(@PathVariable String productOrderId) {
        channelOrderUseCase.releaseExchangeHold(productOrderId);
    }

    @PostMapping("/{productOrderId}/claims/cancel/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "requestSmartStoreSellerCancel")
    public void requestSellerCancel(
            @PathVariable String productOrderId,
            @Valid @RequestBody RequestSmartStoreSellerCancelRequest request) {
        channelOrderUseCase.requestSellerCancel(request.toCommand(productOrderId));
    }
}
