package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase.InquiryWithUser;
import com.personal.happygallery.domain.inquiry.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminInquiryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long userId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String userName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String replyContent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime repliedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static AdminInquiryResponse from(InquiryWithUser iw) {
        Inquiry i = iw.inquiry();
        return new AdminInquiryResponse(
                i.getId(), i.getUserId(), iw.userName(),
                i.getTitle(), i.getContent(),
                i.getReplyContent(), i.getRepliedAt(), i.getCreatedAt());
    }
}
