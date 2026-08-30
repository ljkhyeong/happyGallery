package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.domain.review.ReviewTargetType;

public enum ReviewSourceType {
    ORDER_ITEM,
    BOOKING;

    public static ReviewSourceType from(ReviewTargetType targetType) {
        return switch (targetType) {
            case PRODUCT -> ORDER_ITEM;
            case CLASS -> BOOKING;
        };
    }
}
