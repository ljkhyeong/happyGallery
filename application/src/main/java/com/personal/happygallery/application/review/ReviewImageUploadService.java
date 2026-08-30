package com.personal.happygallery.application.review;

import com.personal.happygallery.application.media.ImageMediaDeletionTransactionService;
import com.personal.happygallery.application.media.UntrustedImageSanitizer;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.domain.review.ReviewImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 비트맵 정제·파일 저장·후기 참조 연결 사이의 보상 경계를 관리한다. */
@Service
class ReviewImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ReviewImageUploadService.class);

    private final ReviewImageAttachmentService attachmentService;
    private final UntrustedImageSanitizer imageSanitizer;
    private final ImageMediaUseCase imageMediaUseCase;
    private final ImageMediaDeletionTransactionService deletionTransaction;

    ReviewImageUploadService(
            ReviewImageAttachmentService attachmentService,
            UntrustedImageSanitizer imageSanitizer,
            ImageMediaUseCase imageMediaUseCase,
            ImageMediaDeletionTransactionService deletionTransaction) {
        this.attachmentService = attachmentService;
        this.imageSanitizer = imageSanitizer;
        this.imageMediaUseCase = imageMediaUseCase;
        this.deletionTransaction = deletionTransaction;
    }

    ReviewImage upload(
            Long userId, Long reviewId, byte[] bytes, String contentType) {
        attachmentService.validateCanAttach(userId, reviewId);
        UntrustedImageSanitizer.SanitizedImage sanitized =
                imageSanitizer.sanitize(bytes, contentType);
        ImageMediaUseCase.StoredImage stored = imageMediaUseCase.upload(
                sanitized.bytes(), sanitized.contentType());
        try {
            return attachmentService.attach(userId, reviewId, stored.url());
        } catch (RuntimeException attachFailure) {
            compensateStoredFile(stored.fileName(), attachFailure);
            throw attachFailure;
        }
    }

    private void compensateStoredFile(String fileName, RuntimeException attachFailure) {
        try {
            deletionTransaction.deleteIfUnreferenced(fileName);
        } catch (RuntimeException cleanupFailure) {
            // 최초 DB 연결 실패를 보존하고, 남은 파일은 7일 고아 정리 배치가 회수한다.
            attachFailure.addSuppressed(cleanupFailure);
            log.warn("후기 이미지 DB 연결 실패 후 파일 보상 삭제 실패 [fileName={} type={}]",
                    fileName, cleanupFailure.getClass().getSimpleName(), cleanupFailure);
        }
    }
}
