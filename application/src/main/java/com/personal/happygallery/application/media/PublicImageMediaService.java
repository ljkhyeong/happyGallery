package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.media.port.in.PublicImageMediaUseCase;
import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
class PublicImageMediaService implements PublicImageMediaUseCase {

    private final ImageMediaUseCase imageMediaUseCase;
    private final ImageMediaReferenceReaderPort referenceReader;
    private final Clock clock;

    PublicImageMediaService(
            ImageMediaUseCase imageMediaUseCase,
            ImageMediaReferenceReaderPort referenceReader,
            Clock clock) {
        this.imageMediaUseCase = imageMediaUseCase;
        this.referenceReader = referenceReader;
        this.clock = clock;
    }

    @Override
    public ImageMediaUseCase.ImageContent get(String fileName) {
        if (!referenceReader.isPubliclyReferenced(
                "/api/v1/media/images/" + fileName,
                LocalDateTime.now(clock))) {
            throw new NotFoundException("이미지");
        }
        return imageMediaUseCase.get(fileName);
    }
}
