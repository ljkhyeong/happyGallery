package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;

public interface ReviewImageMediaUseCase {

    ImageContent getOwnedImage(Long userId, Long reviewId, Long imageId);

    ImageContent getAdminImage(Long reviewId, Long imageId);
}
