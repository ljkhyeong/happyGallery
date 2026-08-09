package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;

/** 후기 목록과 상세 응답에 필요한 현재 이름 기반 조회 모델. */
public record ReviewListView(
        Long id,
        Long userId,
        Long orderItemId,
        Long productId,
        Long bookingId,
        Long bookingClassId,
        String targetName,
        int rating,
        String content,
        ReviewStatus status,
        String hiddenReason,
        LocalDateTime hiddenAt,
        Long hiddenByAdminId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime editedAt,
        String replyContent,
        Long replyAdminId,
        LocalDateTime replyCreatedAt,
        LocalDateTime replyEditedAt
) {

    public ReviewTargetType targetType() {
        return productId != null ? ReviewTargetType.PRODUCT : ReviewTargetType.CLASS;
    }

    public Long sourceId() {
        return orderItemId != null ? orderItemId : bookingId;
    }

    public Long targetId() {
        return productId != null ? productId : bookingClassId;
    }
}
