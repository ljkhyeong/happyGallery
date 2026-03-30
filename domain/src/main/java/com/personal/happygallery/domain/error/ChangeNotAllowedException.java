package com.personal.happygallery.domain.error;

public class ChangeNotAllowedException extends HappyGalleryException {

    public ChangeNotAllowedException() {
        super(ErrorCode.CHANGE_NOT_ALLOWED);
    }
}
