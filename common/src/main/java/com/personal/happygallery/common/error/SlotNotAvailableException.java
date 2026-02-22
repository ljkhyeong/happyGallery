package com.personal.happygallery.common.error;

public class SlotNotAvailableException extends HappyGalleryException {
    public SlotNotAvailableException() {
        super(ErrorCode.SLOT_NOT_AVAILABLE);
    }
}
