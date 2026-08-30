package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public enum BookingSource {
    WEB,
    PHONE,
    NAVER_TALK,
    KAKAO,
    VISIT;

    public void requireOperatorManaged() {
        if (this == WEB) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "운영자 등록 예약은 전화·네이버톡·카카오·방문 접수 경로만 선택할 수 있습니다.");
        }
    }
}
