package com.personal.happygallery.domain.error;

public class CapacityExceededException extends HappyGalleryException {

    public CapacityExceededException() {
        super(ErrorCode.CAPACITY_EXCEEDED);
    }
}
