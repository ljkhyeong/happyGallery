package com.personal.happygallery.domain.error;

public class PhoneVerificationRequiredException extends HappyGalleryException {

    public PhoneVerificationRequiredException() {
        super(ErrorCode.PHONE_VERIFICATION_REQUIRED);
    }
}
