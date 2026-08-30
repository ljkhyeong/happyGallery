package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;

public interface ReviewEvidenceMediaUseCase {

    ImageContent getImage(Long evidenceId, int sortOrder);
}
