package com.personal.happygallery.domain.error;

public class DuplicateBookingException extends HappyGalleryException {
    public DuplicateBookingException() {
        super(ErrorCode.DUPLICATE_BOOKING);
    }
}
