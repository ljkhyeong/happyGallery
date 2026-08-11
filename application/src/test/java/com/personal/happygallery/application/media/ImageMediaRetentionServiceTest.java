package com.personal.happygallery.application.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.media.port.out.ImageMediaReferenceLockPort;
import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageMediaRetentionServiceTest {

    @DisplayName("7일간 참조되지 않은 이미지도 삭제 직전 최신 참조를 다시 확인한다")
    @Test
    void deleteUnreferencedImages_rechecksReferencesBeforeDelete() {
        ImageMediaReferenceReaderPort referenceReader = mock(ImageMediaReferenceReaderPort.class);
        ImageMediaStoragePort storage = mock(ImageMediaStoragePort.class);
        ImageMediaReferenceLockPort referenceLock = mock(ImageMediaReferenceLockPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC);
        when(referenceReader.findReferencedImageUrls()).thenReturn(List.of(
                "/api/v1/media/images/product.png",
                "/api/v1/media/images/class.png"));
        when(referenceReader.findReferencedImageUrls())
                .thenReturn(
                        List.of(
                                "/api/v1/media/images/product.png",
                                "/api/v1/media/images/class.png"),
                        List.of(),
                        List.of("/api/v1/media/images/restored.png?v=1#preview"));
        when(storage.findStoredImageNames())
                .thenReturn(List.of("product.png", "class.png", "orphan.png", "restored.png"));
        when(storage.markOrphanCandidate(
                "orphan.png", clock.instant(), ImageMediaRetentionService.ORPHAN_GRACE_PERIOD))
                .thenReturn(true);
        when(storage.markOrphanCandidate(
                "restored.png", clock.instant(), ImageMediaRetentionService.ORPHAN_GRACE_PERIOD))
                .thenReturn(true);
        ImageMediaReferenceGuard referenceGuard = new ImageMediaReferenceGuard(referenceLock, storage);
        ImageMediaDeletionTransactionService deletionTransaction =
                new ImageMediaDeletionTransactionService(referenceReader, storage, referenceGuard);
        ImageMediaRetentionService service = new ImageMediaRetentionService(
                referenceReader, storage, deletionTransaction, clock);

        int deleted = service.deleteUnreferencedImages();

        assertThat(deleted).isOne();
        verify(referenceLock, times(2)).lock();
        verify(storage).delete("orphan.png");
        verify(storage).clearOrphanMarker("product.png");
        verify(storage).clearOrphanMarker("class.png");
        verify(storage).clearOrphanMarker("restored.png");
        verify(storage, never()).delete("product.png");
        verify(storage, never()).delete("class.png");
        verify(storage, never()).delete("restored.png");
    }

    @DisplayName("삭제 직전 참조 확인은 percent encoding된 로컬 이미지 경로도 같은 파일로 해석한다")
    @Test
    void deleteIfUnreferenced_recognizesPercentEncodedLocalImagePath() {
        ImageMediaReferenceReaderPort referenceReader = mock(ImageMediaReferenceReaderPort.class);
        ImageMediaStoragePort storage = mock(ImageMediaStoragePort.class);
        ImageMediaReferenceLockPort referenceLock = mock(ImageMediaReferenceLockPort.class);
        when(referenceReader.findReferencedImageUrls())
                .thenReturn(List.of("/api/v1/media/images/preserved%2Epng"));
        ImageMediaDeletionTransactionService deletionTransaction =
                new ImageMediaDeletionTransactionService(
                        referenceReader,
                        storage,
                        new ImageMediaReferenceGuard(referenceLock, storage));

        boolean deleted = deletionTransaction.deleteIfUnreferenced("preserved.png");

        assertThat(deleted).isFalse();
        verify(referenceLock).lock();
        verify(storage).clearOrphanMarker("preserved.png");
        verify(storage, never()).delete("preserved.png");
    }

    @DisplayName("같은 커밋에서 제거된 이미지 묶음은 최신 DB 참조를 한 번만 조회해 삭제한다")
    @Test
    void deleteIfUnreferenced_rechecksBatchWithSingleReferenceRead() {
        ImageMediaReferenceReaderPort referenceReader = mock(ImageMediaReferenceReaderPort.class);
        ImageMediaStoragePort storage = mock(ImageMediaStoragePort.class);
        ImageMediaReferenceLockPort referenceLock = mock(ImageMediaReferenceLockPort.class);
        when(referenceReader.findReferencedImageUrls())
                .thenReturn(List.of("/api/v1/media/images/preserved.png"));
        ImageMediaDeletionTransactionService deletionTransaction =
                new ImageMediaDeletionTransactionService(
                        referenceReader,
                        storage,
                        new ImageMediaReferenceGuard(referenceLock, storage));

        int deleted = deletionTransaction.deleteIfUnreferenced(
                List.of("orphan.jpg", "preserved.png"));

        assertThat(deleted).isOne();
        verify(referenceLock).lock();
        verify(referenceReader).findReferencedImageUrls();
        verify(storage).delete("orphan.jpg");
        verify(storage).clearOrphanMarker("preserved.png");
    }
}
