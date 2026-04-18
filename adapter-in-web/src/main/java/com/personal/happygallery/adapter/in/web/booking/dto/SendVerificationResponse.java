package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.PhoneVerification;

public record SendVerificationResponse(
        Long verificationId,
        String phone
) {
    public static SendVerificationResponse from(PhoneVerification pv) {
        return new SendVerificationResponse(pv.getId(), pv.getPhone());
    }
}
