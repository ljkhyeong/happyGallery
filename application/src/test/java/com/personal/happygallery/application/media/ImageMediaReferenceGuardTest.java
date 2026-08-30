package com.personal.happygallery.application.media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageMediaReferenceGuardTest {

    @Test
    @DisplayName("서비스 상대 이미지 경로만 로컬 저장 파일명으로 해석한다")
    void localFileNameAcceptsOnlyServiceRelativePath() {
        assertThat(ImageMediaReferenceGuard.localFileName(
                "/api/v1/media/images/local-image.png?version=1#preview"))
                .isEqualTo("local-image.png");
    }

    @Test
    @DisplayName("같은 경로를 가진 외부 이미지 URL은 로컬 저장 파일로 해석하지 않는다")
    void localFileNameRejectsExternalUrlWithServicePath() {
        assertThat(ImageMediaReferenceGuard.localFileName(
                "https://cdn.example.com/api/v1/media/images/external-image.png"))
                .isNull();
        assertThat(ImageMediaReferenceGuard.localFileName(
                "//cdn.example.com/api/v1/media/images/external-image.png"))
                .isNull();
    }

    @Test
    @DisplayName("파일명이 비어 있는 미디어 경로는 물리 삭제 대상으로 해석하지 않는다")
    void localFileNameRejectsEmptyFileName() {
        assertThat(ImageMediaReferenceGuard.localFileName("/api/v1/media/images/"))
                .isNull();
    }
}
