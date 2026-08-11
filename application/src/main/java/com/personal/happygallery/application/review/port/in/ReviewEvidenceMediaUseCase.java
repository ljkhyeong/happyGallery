package com.personal.happygallery.application.review.port.in;

public interface ReviewEvidenceMediaUseCase {

    record EvidenceImageContent(byte[] bytes, String contentType) {
        public EvidenceImageContent {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    EvidenceImageContent getImage(Long evidenceId, int sortOrder);
}
