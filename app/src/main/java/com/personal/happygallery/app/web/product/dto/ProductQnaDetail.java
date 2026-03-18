package com.personal.happygallery.app.web.product.dto;

import com.personal.happygallery.app.qna.ProductQnaService.QnaWithAuthor;
import com.personal.happygallery.domain.qna.ProductQna;
import java.time.LocalDateTime;

public record ProductQnaDetail(
        Long id, Long productId, String title, String content,
        String replyContent, LocalDateTime repliedAt,
        boolean secret, String authorName, LocalDateTime createdAt
) {
    public static ProductQnaDetail from(QnaWithAuthor qa) {
        ProductQna q = qa.qna();
        return new ProductQnaDetail(
                q.getId(), q.getProductId(), q.getTitle(), q.getContent(),
                q.getReplyContent(), q.getRepliedAt(),
                q.isSecret(), maskName(qa.authorName()), q.getCreatedAt());
    }

    private static String maskName(String name) {
        if (name == null || name.length() <= 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
