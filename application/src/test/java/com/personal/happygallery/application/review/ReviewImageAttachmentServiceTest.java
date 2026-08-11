package com.personal.happygallery.application.review;

import com.personal.happygallery.application.media.ImageMediaReferenceGuard;
import com.personal.happygallery.application.media.ImageMediaReferenceRemovedEvent;
import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewStorePort;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewImage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewImageAttachmentServiceTest {

    @Mock ReviewReaderPort reviewReader;
    @Mock ReviewStorePort reviewStore;
    @Mock ReviewImagePort imagePort;
    @Mock ImageMediaReferenceGuard referenceGuard;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock Review review;

    private ReviewImageAttachmentService service;

    @BeforeEach
    void setUp() {
        service = new ReviewImageAttachmentService(
                reviewReader,
                reviewStore,
                imagePort,
                referenceGuard,
                eventPublisher,
                Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("후기 이미지 참조를 저장하기 전에 전역 미디어 락에서 물리 파일 존재를 검증한다")
    void validatePhysicalFileBeforeSavingReference() {
        String imageUrl = "/api/v1/media/images/11111111-1111-1111-1111-111111111111.jpg";
        when(reviewReader.findByIdAndUserIdForUpdate(2L, 1L)).thenReturn(Optional.of(review));
        when(imagePort.findByReviewId(2L)).thenReturn(List.of());
        when(imagePort.save(any(ReviewImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.attach(1L, 2L, imageUrl);

        var ordered = inOrder(referenceGuard, imagePort);
        ordered.verify(referenceGuard).validateAssignment(imageUrl);
        ordered.verify(imagePort).save(any(ReviewImage.class));
        verify(review).recordContentChange(LocalDateTime.of(2026, 8, 9, 0, 0));
        verify(reviewStore).save(review);
    }

    @Test
    @DisplayName("후기 이미지 참조를 지우면 커밋 후 물리 파일 정리를 위한 이벤트를 발행한다")
    void publishCleanupEventAfterRemovingImageReference() {
        String imageUrl = "/api/v1/media/images/22222222-2222-2222-2222-222222222222.png";
        ReviewImage image = mock(ReviewImage.class);
        when(reviewReader.findByIdAndUserIdForUpdate(2L, 1L)).thenReturn(Optional.of(review));
        when(imagePort.findByIdAndReviewId(3L, 2L)).thenReturn(Optional.of(image));
        when(image.getImageUrl()).thenReturn(imageUrl);

        service.remove(1L, 2L, 3L);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(imagePort).delete(image);
        verify(review).recordContentChange(LocalDateTime.of(2026, 8, 9, 0, 0));
        verify(reviewStore).save(review);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue())
                .isEqualTo(new ImageMediaReferenceRemovedEvent(List.of(imageUrl)));
    }

    @Test
    @DisplayName("후기를 지울 때 연결된 모든 이미지에 커밋 후 물리 파일 정리를 예약한다")
    void publishCleanupEventsAfterRemovingAllReviewImageReferences() {
        ReviewImage first = mock(ReviewImage.class);
        ReviewImage second = mock(ReviewImage.class);
        when(first.getImageUrl()).thenReturn("/api/v1/media/images/first.jpg");
        when(second.getImageUrl()).thenReturn("/api/v1/media/images/second.png");
        when(imagePort.findByReviewId(2L)).thenReturn(List.of(first, second));

        service.removeAll(2L);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(imagePort).deleteByReviewId(2L);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new ImageMediaReferenceRemovedEvent(List.of(
                "/api/v1/media/images/first.jpg",
                "/api/v1/media/images/second.png")));
    }
}
