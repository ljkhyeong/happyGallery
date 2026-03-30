package com.personal.happygallery.domain.error;

public class PaymentMethodNotAllowedException extends HappyGalleryException {

    public PaymentMethodNotAllowedException() {
        super(ErrorCode.PAYMENT_METHOD_NOT_ALLOWED);
    }
}
