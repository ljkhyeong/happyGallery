package com.personal.happygallery.application.media.port.out;

import java.util.List;

public interface ImageMediaReferenceReaderPort {

    record ReferenceVisibility(boolean publiclyReferenced, boolean internallyReferenced) {

        public boolean restrictedToInternalAccess() {
            return internallyReferenced && !publiclyReferenced;
        }
    }

    List<String> findReferencedImageUrls();

    /** 한 로컬 URL이 현재 공개 참조인지, 운영 내부 전용 참조인지 한 번에 판정한다. */
    ReferenceVisibility findReferenceVisibility(String imageUrl);
}
