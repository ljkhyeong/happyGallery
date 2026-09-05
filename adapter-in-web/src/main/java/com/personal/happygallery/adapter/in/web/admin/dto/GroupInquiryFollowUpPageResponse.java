package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record GroupInquiryFollowUpPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<GroupInquiryFollowUp> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore) {
    public record GroupInquiryFollowUp(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String organization,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GroupInquiryStatus status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate nextContactOn) {}

    public static GroupInquiryFollowUpPageResponse from(CursorPage<GroupInquiryUseCase.View> page) {
        return new GroupInquiryFollowUpPageResponse(page.content().stream().map(view -> new GroupInquiryFollowUp(
                view.inquiry().getId(), view.details().organization(), view.inquiry().getStatus(), view.inquiry().getNextContactOn())).toList(),
                page.nextCursor(), page.hasMore());
    }
}
