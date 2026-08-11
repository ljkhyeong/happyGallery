package com.personal.happygallery.application.review.port.in;

public interface ReviewImageMediaUseCase {

    record ReviewImageContent(byte[] bytes, String contentType) {
        public ReviewImageContent {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    ReviewImageContent getOwnedImage(Long userId, Long reviewId, Long imageId);

    ReviewImageContent getAdminImage(Long reviewId, Long imageId);
}
