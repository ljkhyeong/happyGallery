package com.personal.happygallery.common.error;

public class AlreadyRefundedException extends HappyGalleryException {

    public AlreadyRefundedException() {
        super(ErrorCode.ALREADY_REFUNDED);
    }
}
