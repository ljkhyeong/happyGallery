package com.personal.happygallery.app.web.booking.dto;

import com.personal.happygallery.domain.booking.PhoneVerification;

public record SendVerificationResponse(
        Long verificationId,
        String phone,
        String code  // MVP only: 실제 SMS 발송 구현 시 제거
) {
    public static SendVerificationResponse from(PhoneVerification pv) {
        return new SendVerificationResponse(pv.getId(), pv.getPhone(), pv.getCode());
    }
}
