package com.personal.happygallery.common.error;

public class PassExpiredException extends HappyGalleryException {

    public PassExpiredException() {
        super(ErrorCode.PASS_EXPIRED);
    }
}
