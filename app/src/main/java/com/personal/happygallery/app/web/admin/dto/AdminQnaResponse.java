package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.app.qna.ProductQnaService.QnaWithAuthor;
import com.personal.happygallery.domain.qna.ProductQna;
import java.time.LocalDateTime;

public record AdminQnaResponse(
        Long id, Long productId, Long userId, String authorName,
        String title, String content, boolean secret,
        String replyContent, LocalDateTime repliedAt, LocalDateTime createdAt
) {
    public static AdminQnaResponse from(QnaWithAuthor qa) {
        ProductQna q = qa.qna();
        return new AdminQnaResponse(
                q.getId(), q.getProductId(), q.getUserId(), qa.authorName(),
                q.getTitle(), q.getContent(), q.isSecret(),
                q.getReplyContent(), q.getRepliedAt(), q.getCreatedAt());
    }
}
