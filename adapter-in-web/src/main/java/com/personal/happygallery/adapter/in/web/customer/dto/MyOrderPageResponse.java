package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.order.port.in.OrderQueryUseCase.OrderSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyOrderPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<MyOrderSummary> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static MyOrderPageResponse from(CursorPage<OrderSummary> page) {
        return new MyOrderPageResponse(
                MyOrderSummary.fromAll(page.content()),
                page.nextCursor(),
                page.hasMore());
    }
}
