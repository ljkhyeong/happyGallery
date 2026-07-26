package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.order.dto.OrderClaimResponse;
import com.personal.happygallery.application.order.port.in.OrderClaimView;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminOrderClaimPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<OrderClaimResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {

    public static AdminOrderClaimPageResponse from(CursorPage<OrderClaimView> page) {
        return new AdminOrderClaimPageResponse(
                OrderClaimResponse.fromAll(page.content()),
                page.nextCursor(),
                page.hasMore());
    }
}
