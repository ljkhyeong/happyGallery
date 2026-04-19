package com.personal.happygallery.adapter.in.web.product.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskName;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase.QnaWithAuthor;
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
}
