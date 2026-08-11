package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.media.port.in.PublicImageMediaUseCase;
import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import org.springframework.stereotype.Service;

@Service
class PublicImageMediaService implements PublicImageMediaUseCase {

    private final ImageMediaUseCase imageMediaUseCase;
    private final ImageMediaReferenceReaderPort referenceReader;

    PublicImageMediaService(
            ImageMediaUseCase imageMediaUseCase,
            ImageMediaReferenceReaderPort referenceReader) {
        this.imageMediaUseCase = imageMediaUseCase;
        this.referenceReader = referenceReader;
    }

    @Override
    public ImageMediaUseCase.ImageContent get(String fileName) {
        var visibility = referenceReader.findReferenceVisibility(
                "/api/v1/media/images/" + fileName);
        if (visibility.restrictedToInternalAccess()) {
            throw new NotFoundException("이미지");
        }
        // 내부 전용 참조가 없는 업로드 직후 관리자 미리보기는 기존처럼 허용한다.
        return imageMediaUseCase.get(fileName);
    }
}
