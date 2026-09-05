package com.personal.happygallery.domain.inquiry;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.EmailAddress;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;

public record GroupInquiryDetails(String organization, String contactName, String phone, String email,
        int headcount, String preferredSchedule, String location, String classInterest, String message) {
    public GroupInquiryDetails {
        organization = ContentTextPolicy.requireTitle(organization, "기관·모임명").strip();
        contactName = PersonalName.required(contactName);
        phone = KoreanPhoneNumber.required(phone);
        email = EmailAddress.optional(email);
        if (headcount < 1 || headcount > 500) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "참여 인원은 1~500명이어야 합니다.");
        }
        preferredSchedule = ContentTextPolicy.requireTitle(preferredSchedule, "희망 일정").strip();
        location = ContentTextPolicy.requireTitle(location, "수업 장소").strip();
        classInterest = ContentTextPolicy.requireTitle(classInterest, "관심 수업").strip();
        message = optionalMessage(message);
    }

    public static String optionalMessage(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > 2000) throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "상담 내용은 2000자 이하여야 합니다.");
        return value.strip();
    }
}
