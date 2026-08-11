package com.personal.happygallery.application.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.media.ImageMediaReferenceRemovedEvent;
import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.application.review.port.out.ReviewModerationPort;
import com.personal.happygallery.application.review.port.out.ReviewReportPort;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ReviewEvidenceRetentionServiceTest {

    @Test
    @DisplayName("만료 증거 삭제 뒤 그 증거가 보존하던 이미지 URL 묶음을 커밋 후 정리 이벤트로 발행한다")
    void publishRemovedEvidenceImageUrlsAsOneBatch() {
        ReviewEvidencePort evidencePort = mock(ReviewEvidencePort.class);
        ReviewModerationPort moderationPort = mock(ReviewModerationPort.class);
        ReviewReportPort reportPort = mock(ReviewReportPort.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ReviewEvidenceSnapshot first = mock(ReviewEvidenceSnapshot.class);
        ReviewEvidenceSnapshot second = mock(ReviewEvidenceSnapshot.class);
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 0, 0);
        when(moderationPort.findBefore(now.minusYears(3), 10)).thenReturn(List.of());
        when(reportPort.findResolvedBefore(now.minusYears(3), 10)).thenReturn(List.of());
        when(evidencePort.findExpired(now, 10)).thenReturn(List.of(first, second));
        when(first.getId()).thenReturn(1L);
        when(second.getId()).thenReturn(2L);
        when(evidencePort.findByIds(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(first.getImageUrls()).thenReturn(List.of(
                "/api/v1/media/images/first.jpg",
                "/api/v1/media/images/shared.png"));
        when(second.getImageUrls()).thenReturn(List.of(
                "/api/v1/media/images/shared.png",
                "/api/v1/media/images/second.webp"));
        ReviewEvidenceRetentionService service = new ReviewEvidenceRetentionService(
                evidencePort, moderationPort, reportPort, publisher);

        int deleted = service.deleteExpiredBatch(now, 10);

        assertThat(deleted).isEqualTo(2);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        var ordered = inOrder(evidencePort, publisher);
        ordered.verify(evidencePort).deleteAll(List.of(first, second));
        ordered.verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new ImageMediaReferenceRemovedEvent(List.of(
                "/api/v1/media/images/first.jpg",
                "/api/v1/media/images/shared.png",
                "/api/v1/media/images/second.webp")));
        verify(evidencePort).findExpired(now, 10);
    }
}
