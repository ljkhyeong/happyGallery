package com.personal.happygallery.common.error;

public class PhoneVerificationFailedException extends HappyGalleryException {
    public PhoneVerificationFailedException() {
        super(ErrorCode.PHONE_VERIFICATION_FAILED);
    }
}
