package com.personal.happygallery.domain.error;

public class RefundNotAllowedException extends HappyGalleryException {

    public RefundNotAllowedException() {
        super(ErrorCode.REFUND_NOT_ALLOWED);
    }
}
