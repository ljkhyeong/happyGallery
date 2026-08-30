package com.personal.happygallery.domain.order;

/** 배송조회 등록 작업 상태. */
public enum TrackingRegistrationStatus {
    PENDING,
    PROCESSING,
    ACTIVE,
    COMPLETED,
    FAILED
}
