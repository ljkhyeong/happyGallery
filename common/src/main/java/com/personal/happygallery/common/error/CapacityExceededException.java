package com.personal.happygallery.common.error;

public class CapacityExceededException extends HappyGalleryException {

    public CapacityExceededException() {
        super(ErrorCode.CAPACITY_EXCEEDED);
    }
}
