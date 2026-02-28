package com.personal.happygallery.common.error;

public class PassCreditInsufficientException extends HappyGalleryException {

    public PassCreditInsufficientException() {
        super(ErrorCode.PASS_CREDIT_INSUFFICIENT);
    }
}
