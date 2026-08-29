package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.ResolveSmartStoreReturnRequest;
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
}
