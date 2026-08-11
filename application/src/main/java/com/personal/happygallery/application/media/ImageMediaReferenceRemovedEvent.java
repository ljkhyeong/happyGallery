package com.personal.happygallery.application.media;

import java.util.List;

/** DB 미디어 참조가 제거된 트랜잭션에서 발행하는 물리 파일 정리 신호. */
public record ImageMediaReferenceRemovedEvent(List<String> imageUrls) {

    public ImageMediaReferenceRemovedEvent {
        imageUrls = List.copyOf(imageUrls);
    }
}
