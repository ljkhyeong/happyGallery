package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.pass.port.in.PassQueryUseCase.PassView;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyPassPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<MyPassSummary> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static MyPassPageResponse from(CursorPage<PassView> page) {
        return new MyPassPageResponse(
                MyPassSummary.fromAll(page.content()),
                page.nextCursor(),
                page.hasMore());
    }
}
