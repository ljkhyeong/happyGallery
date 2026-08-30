package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.product.port.in.SmartStoreProductNoticeUseCase.SaveCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SaveSmartStoreNoticeRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PostCategoryType postCategoryType,
        @NotBlank String title,
        boolean importantNotice,
        OffsetDateTime importantNoticeStartDate,
        OffsetDateTime importantNoticeEndDate,
        boolean wholeNotice,
        OffsetDateTime displayStartDate,
        OffsetDateTime displayEndDate,
        boolean popup,
        OffsetDateTime popupStartDate,
        OffsetDateTime popupEndDate,
        @NotBlank String detailContents
) {
    public SaveCommand toCommand() {
        return new SaveCommand(
                postCategoryType.name(), title, importantNotice,
                importantNoticeStartDate, importantNoticeEndDate, wholeNotice,
                displayStartDate, displayEndDate, popup, popupStartDate, popupEndDate,
                detailContents);
    }

    public enum PostCategoryType {
        ORDINARY,
        EVENT,
        DELIVERY,
        PRODUCT
    }
}
