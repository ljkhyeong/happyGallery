package com.personal.happygallery.application.media.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface ImageMediaReferenceReaderPort {

    List<String> findReferencedImageUrls();

    /** 한 로컬 URL을 현재 공개 aggregate가 참조하는지 판정한다. */
    boolean isPubliclyReferenced(String imageUrl, LocalDateTime now);
}
