package com.personal.happygallery.common.error;

public class PaymentMethodNotAllowedException extends HappyGalleryException {

    public PaymentMethodNotAllowedException() {
        super(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED);
    }
}
