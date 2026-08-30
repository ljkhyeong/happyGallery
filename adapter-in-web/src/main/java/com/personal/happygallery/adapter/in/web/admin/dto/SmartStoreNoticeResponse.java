package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreProductNoticeUseCase.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record SmartStoreNoticeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sellerNoticeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String postCategoryType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean importantNotice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime importantNoticeStartDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime importantNoticeEndDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean wholeNotice,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime displayStartDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime displayEndDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean popup,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime popupStartDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) OffsetDateTime popupEndDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String detailContents
) {
    public static SmartStoreNoticeResponse from(Notice notice) {
        return new SmartStoreNoticeResponse(
                notice.sellerNoticeId(), notice.postCategoryType(), notice.title(),
                notice.importantNotice(), notice.importantNoticeStartDate(),
                notice.importantNoticeEndDate(), notice.wholeNotice(), notice.displayStartDate(),
                notice.displayEndDate(), notice.popup(), notice.popupStartDate(),
                notice.popupEndDate(), notice.detailContents());
    }
}
