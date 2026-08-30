package com.personal.happygallery.application.review;

import com.personal.happygallery.application.media.ImageMediaReferenceGuard;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;
import com.personal.happygallery.application.review.port.in.ReviewEvidenceMediaUseCase;
import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReviewEvidenceMediaService implements ReviewEvidenceMediaUseCase {

    private final ReviewEvidencePort evidencePort;
    private final ImageMediaUseCase imageMediaUseCase;

    ReviewEvidenceMediaService(
            ReviewEvidencePort evidencePort,
            ImageMediaUseCase imageMediaUseCase) {
        this.evidencePort = evidencePort;
        this.imageMediaUseCase = imageMediaUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public ImageContent getImage(Long evidenceId, int sortOrder) {
        ReviewEvidenceSnapshot evidence = evidencePort.findById(evidenceId)
                .orElseThrow(NotFoundException.supplier("후기 증거"));
        List<String> imageUrls = evidence.getImageUrls();
        if (sortOrder < 0 || sortOrder >= imageUrls.size()) {
            throw new NotFoundException("후기 증거 이미지");
        }

        String fileName = ImageMediaReferenceGuard.localFileName(imageUrls.get(sortOrder));
        if (fileName == null) {
            throw new NotFoundException("후기 증거 이미지");
        }
        return imageMediaUseCase.get(fileName);
    }
}
