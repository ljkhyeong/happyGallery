package com.personal.happygallery.domain.inquiry;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public enum GroupInquiryStatus {
    RECEIVED, CONSULTING, CONFIRMED, CLOSED;

    public void requireTransitionTo(GroupInquiryStatus next) {
        boolean allowed = next != null && (this == next || switch (this) {
            case RECEIVED -> next == CONSULTING || next == CLOSED;
            case CONSULTING -> next == CONFIRMED || next == CLOSED;
            case CONFIRMED, CLOSED -> next == CONSULTING || next == CLOSED;
        });
        if (!allowed) throw new HappyGalleryException(ErrorCode.CONFLICT, "상담을 시작한 뒤 확정할 수 있습니다.");
    }
}
