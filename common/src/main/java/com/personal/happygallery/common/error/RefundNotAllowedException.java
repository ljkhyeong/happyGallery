package com.personal.happygallery.common.error;

public class RefundNotAllowedException extends HappyGalleryException {

    public RefundNotAllowedException() {
        super(ErrorCode.REFUND_NOT_ALLOWED);
    }
}
