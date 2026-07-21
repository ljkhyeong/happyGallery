package com.personal.happygallery.application.media.port.in;

public interface ImageMediaUseCase {

    record StoredImage(String fileName, String url) {}

    record ImageContent(byte[] bytes, String contentType) {
        public ImageContent {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    StoredImage upload(byte[] bytes, String contentType);

    ImageContent get(String fileName);
}
