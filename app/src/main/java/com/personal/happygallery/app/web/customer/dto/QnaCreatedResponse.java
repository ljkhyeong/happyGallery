package com.personal.happygallery.app.web.customer.dto;

import com.personal.happygallery.domain.qna.ProductQna;
import java.time.LocalDateTime;

public record QnaCreatedResponse(Long id, Long productId, String title,
                                 boolean secret, LocalDateTime createdAt) {
    public static QnaCreatedResponse from(ProductQna q) {
        return new QnaCreatedResponse(q.getId(), q.getProductId(),
                q.getTitle(), q.isSecret(), q.getCreatedAt());
    }
}
