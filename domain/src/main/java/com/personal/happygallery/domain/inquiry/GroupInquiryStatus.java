package com.personal.happygallery.domain.inquiry;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public enum GroupInquiryStatus {
    RECEIVED, CONSULTING, CONFIRMED, CLOSED, CANCELED;

    public void requireMemberChange() {
        if (this != RECEIVED && this != CONSULTING) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "확정 전 문의만 수정하거나 취소할 수 있습니다.");
        }
    }

    public void requireTransitionTo(GroupInquiryStatus next) {
        boolean allowed = next != null && (this == next && this != CANCELED || switch (this) {
            case RECEIVED -> next == CONSULTING || next == CLOSED;
            case CONSULTING -> next == CONFIRMED || next == CLOSED;
            case CANCELED -> false;
            case CONFIRMED, CLOSED -> next == CONSULTING || next == CLOSED;
        });
        if (!allowed) throw new HappyGalleryException(ErrorCode.CONFLICT, "현재 문의 상태에서는 선택한 상태로 변경할 수 없습니다.");
    }
}
