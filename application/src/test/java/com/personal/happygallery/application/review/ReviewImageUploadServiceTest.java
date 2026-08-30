package com.personal.happygallery.application.review;

import com.personal.happygallery.application.media.ImageMediaDeletionTransactionService;
import com.personal.happygallery.application.media.UntrustedImageSanitizer;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.domain.review.ReviewImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewImageUploadServiceTest {

    @Mock ReviewImageAttachmentService attachmentService;
    @Mock UntrustedImageSanitizer imageSanitizer;
    @Mock ImageMediaUseCase imageMediaUseCase;
    @Mock ImageMediaDeletionTransactionService deletionTransaction;

    private ReviewImageUploadService service;

    @BeforeEach
    void setUp() {
        service = new ReviewImageUploadService(
                attachmentService, imageSanitizer, imageMediaUseCase, deletionTransaction);
    }

    @Test
    @DisplayName("파일 저장 뒤 후기 이미지 DB 연결이 실패하면 참조를 재확인해 물리 파일을 보상 삭제한다")
    void compensateStoredFileWhenDatabaseAttachmentFails() {
        byte[] original = {1};
        byte[] sanitized = {2};
        RuntimeException attachFailure = new IllegalStateException("DB attach failed");
        when(imageSanitizer.sanitize(original, "image/jpeg"))
                .thenReturn(new UntrustedImageSanitizer.SanitizedImage(sanitized, "image/jpeg"));
        when(imageMediaUseCase.upload(aryEq(sanitized), eq("image/jpeg")))
                .thenReturn(new ImageMediaUseCase.StoredImage(
                        "11111111-1111-1111-1111-111111111111.jpg",
                        "/api/v1/media/images/11111111-1111-1111-1111-111111111111.jpg"));
        when(attachmentService.attach(
                1L,
                2L,
                "/api/v1/media/images/11111111-1111-1111-1111-111111111111.jpg"))
                .thenThrow(attachFailure);

        assertThatThrownBy(() -> service.upload(1L, 2L, original, "image/jpeg"))
                .isSameAs(attachFailure);

        verify(deletionTransaction)
                .deleteIfUnreferenced("11111111-1111-1111-1111-111111111111.jpg");
    }

    @Test
    @DisplayName("후기 이미지 DB 연결이 성공하면 저장한 물리 파일을 삭제하지 않는다")
    void keepStoredFileWhenDatabaseAttachmentSucceeds() {
        byte[] original = {1};
        byte[] sanitized = {2};
        ReviewImage image = mock(ReviewImage.class);
        when(imageSanitizer.sanitize(original, null))
                .thenReturn(new UntrustedImageSanitizer.SanitizedImage(sanitized, "image/png"));
        when(imageMediaUseCase.upload(aryEq(sanitized), eq("image/png")))
                .thenReturn(new ImageMediaUseCase.StoredImage(
                        "22222222-2222-2222-2222-222222222222.png",
                        "/api/v1/media/images/22222222-2222-2222-2222-222222222222.png"));
        when(attachmentService.attach(
                1L,
                2L,
                "/api/v1/media/images/22222222-2222-2222-2222-222222222222.png"))
                .thenReturn(image);

        service.upload(1L, 2L, original, null);

        verify(deletionTransaction, never()).deleteIfUnreferenced(
                "22222222-2222-2222-2222-222222222222.png");
    }
}
