package com.personal.happygallery.application.media.port.out;

import java.util.List;

public interface ImageMediaReferenceReaderPort {

    List<String> findReferencedImageUrls();
}
