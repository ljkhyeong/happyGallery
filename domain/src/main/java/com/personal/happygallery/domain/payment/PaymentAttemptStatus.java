package com.personal.happygallery.domain.payment;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public enum PaymentAttemptStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    CANCELED;

    /** confirm 호출 시점에 PENDING 상태가 아니면 {@link ErrorCode#INVALID_INPUT}을 던진다. */
    public void requireConfirmable() {
        if (this != PENDING) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미 처리된 결제입니다.");
        }
    }
}
