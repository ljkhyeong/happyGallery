package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.inquiry.Inquiry;
import java.time.LocalDateTime;

public record InquiryResponse(
        Long id, String title, String content,
        boolean hasReply, String replyContent, LocalDateTime repliedAt,
        LocalDateTime createdAt
) {
    public static InquiryResponse from(Inquiry i) {
        return new InquiryResponse(
                i.getId(), i.getTitle(), i.getContent(),
                i.hasReply(), i.getReplyContent(), i.getRepliedAt(),
                i.getCreatedAt());
    }
}
