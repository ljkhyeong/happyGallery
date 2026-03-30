package com.personal.happygallery.domain.error;

public class AlreadyRefundedException extends HappyGalleryException {

    public AlreadyRefundedException() {
        super(ErrorCode.ALREADY_REFUNDED);
    }
}
