package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.inquiry.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record InquiryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasReply,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String replyContent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime repliedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static InquiryResponse from(Inquiry i) {
        return new InquiryResponse(
                i.getId(), i.getTitle(), i.getContent(),
                i.hasReply(), i.getReplyContent(), i.getRepliedAt(),
                i.getCreatedAt());
    }
}
