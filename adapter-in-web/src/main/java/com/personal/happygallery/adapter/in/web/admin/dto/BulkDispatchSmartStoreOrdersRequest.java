package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.DispatchCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record BulkDispatchSmartStoreOrdersRequest(
        @NotNull @Size(min = 1, max = 30) List<@Valid DispatchOrder> orders
) {
    public List<DispatchCommand> toCommands() {
        return orders.stream().map(DispatchOrder::toCommand).toList();
    }

    public record DispatchOrder(
            @NotBlank String productOrderId,
            @NotBlank String deliveryMethod,
            String deliveryCompanyCode,
            String trackingNumber,
            @NotNull LocalDateTime dispatchDate
    ) {
        private DispatchCommand toCommand() {
            return new DispatchCommand(
                    productOrderId, deliveryMethod, deliveryCompanyCode,
                    trackingNumber, dispatchDate);
        }
    }
}
