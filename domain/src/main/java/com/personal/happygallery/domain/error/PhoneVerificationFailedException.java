package com.personal.happygallery.domain.error;

public class PhoneVerificationFailedException extends HappyGalleryException {
    public PhoneVerificationFailedException() {
        super(ErrorCode.PHONE_VERIFICATION_FAILED);
    }
}
