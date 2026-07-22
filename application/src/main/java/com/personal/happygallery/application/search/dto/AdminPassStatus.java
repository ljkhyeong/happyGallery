package com.personal.happygallery.application.search.dto;

import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;

/** 관리자 조회에서 8회권의 사용·환불 상태를 한눈에 구분하기 위한 파생 상태. */
public enum AdminPassStatus {
    ACTIVE,
    USED_UP,
    EXPIRED,
    REFUND_PENDING,
    REFUND_FAILED,
    REFUNDED;

    public static AdminPassStatus from(RefundStatus refundStatus,
                                       LocalDateTime expiresAt,
                                       int remainingCredits,
                                       LocalDateTime now) {
        if (refundStatus != null) {
            return switch (refundStatus) {
                case SUCCEEDED -> REFUNDED;
                case FAILED -> REFUND_FAILED;
                case REQUESTED, PROCESSING, RETRYABLE, RECONCILIATION_REQUIRED -> REFUND_PENDING;
            };
        }
        if (!now.isBefore(expiresAt)) {
            return EXPIRED;
        }
        return remainingCredits > 0 ? ACTIVE : USED_UP;
    }
}
