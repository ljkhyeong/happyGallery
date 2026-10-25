package com.personal.happygallery.domain.inquiry;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.text.DomainText;
import com.personal.happygallery.domain.user.EmailAddress;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.PersonalName;

public record GroupInquiryDetails(String organization, String contactName, String phone, String email,
        int headcount, String preferredSchedule, String location, String classInterest, String message) {
    public GroupInquiryDetails {
        organization = DomainText.required(
                organization, "기관·모임명은", ContentTextPolicy.MAX_TITLE_LENGTH);
        contactName = PersonalName.required(contactName);
        phone = KoreanPhoneNumber.required(phone);
        email = EmailAddress.optional(email);
        if (headcount < 1 || headcount > 500) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "참여 인원은 1~500명이어야 합니다.");
        }
        preferredSchedule = DomainText.required(
                preferredSchedule, "희망 일정은", ContentTextPolicy.MAX_TITLE_LENGTH);
        // ContentTextPolicy가 만들던 "수업 장소은" 문구를 그대로 유지한다.
        location = DomainText.required(location, "수업 장소은", ContentTextPolicy.MAX_TITLE_LENGTH);
        classInterest = DomainText.required(
                classInterest, "관심 수업은", ContentTextPolicy.MAX_TITLE_LENGTH);
        message = optionalMessage(message);
    }

    /** 상담 내용과 상담 메모의 공통 정규화. 비어 있으면 null. */
    public static String optionalMessage(String value) {
        return DomainText.optional(value, "상담 내용은", 2000);
    }
}
