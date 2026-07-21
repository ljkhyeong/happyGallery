package com.personal.happygallery.application.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import com.personal.happygallery.application.media.port.out.ImageMediaReferenceLockPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
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
        ProductReaderPort productReader = mock(ProductReaderPort.class);
        ClassReaderPort classReader = mock(ClassReaderPort.class);
        ImageMediaStoragePort storage = mock(ImageMediaStoragePort.class);
        ImageMediaReferenceLockPort referenceLock = mock(ImageMediaReferenceLockPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC);
        Product product = new Product(
                "가죽 소품", ProductType.READY_STOCK, null, 10_000, null,
                "/api/v1/media/images/product.png");
        BookingClass bookingClass = new BookingClass(
                "레진아트", "RESIN", 60, 30_000, 30, true,
                null, "/api/v1/media/images/class.png", null, null);
        Product restoredProduct = new Product(
                "위빙 소품", ProductType.READY_STOCK, null, 20_000, null,
                "/api/v1/media/images/restored.png");
        when(productReader.findAllProductsByCreatedAtDesc())
                .thenReturn(List.of(product), List.of(product, restoredProduct));
        when(classReader.findAll()).thenReturn(List.of(bookingClass));
        when(storage.findStoredImageNames())
                .thenReturn(List.of("product.png", "class.png", "orphan.png", "restored.png"));
        when(storage.markOrphanCandidate(
                "orphan.png", clock.instant(), ImageMediaRetentionService.ORPHAN_GRACE_PERIOD))
                .thenReturn(true);
        when(storage.markOrphanCandidate(
                "restored.png", clock.instant(), ImageMediaRetentionService.ORPHAN_GRACE_PERIOD))
                .thenReturn(true);
        ImageMediaRetentionService service = new ImageMediaRetentionService(
                productReader, classReader,
                storage, new ImageMediaReferenceGuard(referenceLock, storage), clock);

        int deleted = service.deleteUnreferencedImages();

        assertThat(deleted).isOne();
        verify(referenceLock).lock();
        verify(storage).delete("orphan.png");
        verify(storage).clearOrphanMarker("product.png");
        verify(storage).clearOrphanMarker("class.png");
        verify(storage).clearOrphanMarker("restored.png");
        verify(storage, never()).delete("product.png");
        verify(storage, never()).delete("class.png");
        verify(storage, never()).delete("restored.png");
    }
}
