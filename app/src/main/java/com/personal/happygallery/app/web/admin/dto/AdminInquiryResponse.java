package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.app.inquiry.port.in.InquiryUseCase.InquiryWithUser;
import com.personal.happygallery.domain.inquiry.Inquiry;
import java.time.LocalDateTime;

public record AdminInquiryResponse(
        Long id, Long userId, String userName,
        String title, String content,
        String replyContent, LocalDateTime repliedAt, LocalDateTime createdAt
) {
    public static AdminInquiryResponse from(InquiryWithUser iw) {
        Inquiry i = iw.inquiry();
        return new AdminInquiryResponse(
                i.getId(), i.getUserId(), iw.userName(),
                i.getTitle(), i.getContent(),
                i.getReplyContent(), i.getRepliedAt(), i.getCreatedAt());
    }
}
