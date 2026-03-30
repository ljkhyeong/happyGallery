package com.personal.happygallery.domain.error;

public class PhoneVerificationRequiredException extends HappyGalleryException {

    public PhoneVerificationRequiredException() {
        super(ErrorCode.INVALID_INPUT, "휴대폰 인증을 완료한 뒤 다시 시도해주세요.");
    }
}
