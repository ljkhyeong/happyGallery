package com.personal.happygallery.app.web.product.dto;

import static com.personal.happygallery.app.web.MaskingUtil.maskName;

import com.personal.happygallery.app.qna.port.in.ProductQnaUseCase.QnaWithAuthor;
import com.personal.happygallery.domain.qna.ProductQna;
import java.time.LocalDateTime;

public record ProductQnaListItem(
        Long id, String title, String authorName, boolean secret,
        boolean hasReply, LocalDateTime createdAt
) {
    public static ProductQnaListItem from(QnaWithAuthor qa) {
        ProductQna q = qa.qna();
        String displayTitle = q.isSecret() ? "[비밀글입니다]" : q.getTitle();
        return new ProductQnaListItem(
                q.getId(), displayTitle, maskName(qa.authorName()),
                q.isSecret(), q.hasReply(), q.getCreatedAt());
    }
}
