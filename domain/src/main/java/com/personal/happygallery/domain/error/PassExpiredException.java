package com.personal.happygallery.domain.error;

public class PassExpiredException extends HappyGalleryException {

    public PassExpiredException() {
        super(ErrorCode.PASS_EXPIRED);
    }
}
