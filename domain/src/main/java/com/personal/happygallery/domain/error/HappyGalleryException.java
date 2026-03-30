package com.personal.happygallery.domain.error;

public class HappyGalleryException extends RuntimeException {

    private final ErrorCode errorCode;

    public HappyGalleryException(ErrorCode errorCode) {
        this(errorCode, errorCode.message);
    }

    public HappyGalleryException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
