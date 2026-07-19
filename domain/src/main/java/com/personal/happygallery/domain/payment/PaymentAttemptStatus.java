package com.personal.happygallery.domain.payment;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public enum PaymentAttemptStatus {
    PENDING,
    PROCESSING,
    RETRYABLE,
    APPROVED,
    CONFIRMED,
    FAILED,
    RECONCILIATION_REQUIRED,
    COMPENSATION_REQUESTED,
    COMPENSATION_FAILED,
    COMPENSATED,
    CANCELED;

    /** confirm 선점은 최초 요청이나 재시도 가능한 PG 실패에서만 허용한다. */
    public void requireConfirmable() {
        if (this != PENDING && this != RETRYABLE) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미 처리된 결제입니다.");
        }
    }
}
