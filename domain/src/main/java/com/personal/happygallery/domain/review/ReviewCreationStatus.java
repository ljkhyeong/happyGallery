package com.personal.happygallery.domain.review;

/** 특정 완료 거래 원천의 후기 작성 가능 상태. */
public enum ReviewCreationStatus {
    AVAILABLE,
    REVIEW_EXISTS,
    RECREATION_BLOCKED,
    NOT_REVIEWABLE
}
