package com.personal.happygallery.common.error;

public class HappyGalleryException extends RuntimeException {

    private final ErrorCode errorCode;

    public HappyGalleryException(ErrorCode errorCode) {
        super(errorCode.message);
        this.errorCode = errorCode;
    }

    public HappyGalleryException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
