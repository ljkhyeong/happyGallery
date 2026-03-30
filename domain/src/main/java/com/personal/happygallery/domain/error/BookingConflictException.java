package com.personal.happygallery.domain.error;

public class BookingConflictException extends HappyGalleryException {
    public BookingConflictException() {
        super(ErrorCode.BOOKING_CONFLICT);
    }
}
