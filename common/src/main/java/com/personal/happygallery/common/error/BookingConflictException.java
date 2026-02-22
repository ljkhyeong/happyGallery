package com.personal.happygallery.common.error;

public class BookingConflictException extends HappyGalleryException {
    public BookingConflictException() {
        super(ErrorCode.BOOKING_CONFLICT);
    }
}
