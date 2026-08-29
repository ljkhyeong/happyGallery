package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.DelaySmartStoreOrderRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.DispatchSmartStoreOrderRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.DispatchSmartStoreExchangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.ResolveSmartStoreReturnRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreChannelOrderDetailResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreChannelOrderResponse;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

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
                channelOrderUseCase.resolveReturn(productOrderId, request.restoreStock()));
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

    @PostMapping("/{productOrderId}/claims/exchange/dispatch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "dispatchSmartStoreExchangeClaim")
    public void dispatchExchange(
            @PathVariable String productOrderId,
            @Valid @RequestBody DispatchSmartStoreExchangeRequest request) {
        channelOrderUseCase.dispatchExchange(request.toCommand(productOrderId));
    }
}
