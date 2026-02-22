package com.personal.happygallery.common.error;

public class DuplicateBookingException extends HappyGalleryException {
    public DuplicateBookingException() {
        super(ErrorCode.DUPLICATE_BOOKING);
    }
}
