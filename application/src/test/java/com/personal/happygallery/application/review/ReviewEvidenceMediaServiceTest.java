package com.personal.happygallery.application.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewEvidenceMediaServiceTest {

    @Test
    @DisplayName("존재하지 않는 증거 ID는 저장소 존재 여부를 확인하지 않고 찾을 수 없다고 응답한다")
    void rejectUnknownEvidence() {
        ReviewEvidencePort evidencePort = mock(ReviewEvidencePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        when(evidencePort.findById(7L)).thenReturn(Optional.empty());
        ReviewEvidenceMediaService service = new ReviewEvidenceMediaService(evidencePort, media);

        assertThatThrownBy(() -> service.getImage(7L, 0))
                .isInstanceOf(NotFoundException.class);
        verify(media, never()).get(anyString());
    }

    @Test
    @DisplayName("증거 이미지 순번이 가리키는 로컬 파일만 저장소에서 읽는다")
    void readImageOwnedByEvidenceAtSortOrder() {
        ReviewEvidencePort evidencePort = mock(ReviewEvidencePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ReviewEvidenceSnapshot evidence = mock(ReviewEvidenceSnapshot.class);
        String fileName = "11111111-1111-1111-1111-111111111111.png";
        when(evidencePort.findById(7L)).thenReturn(Optional.of(evidence));
        when(evidence.getImageUrls()).thenReturn(List.of(
                "/api/v1/media/images/first.jpg",
                "/api/v1/media/images/" + fileName));
        when(media.get(fileName)).thenReturn(new ImageMediaUseCase.ImageContent(
                new byte[] {4, 5, 6}, "image/png"));
        ReviewEvidenceMediaService service = new ReviewEvidenceMediaService(evidencePort, media);

        var image = service.getImage(7L, 1);

        assertThat(image.bytes()).containsExactly(4, 5, 6);
        assertThat(image.contentType()).isEqualTo("image/png");
        verify(media).get(fileName);
    }

    @Test
    @DisplayName("증거에 없는 이미지 순번은 저장소 존재 여부를 확인하지 않고 찾을 수 없다고 응답한다")
    void rejectSortOrderNotOwnedByEvidence() {
        ReviewEvidencePort evidencePort = mock(ReviewEvidencePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ReviewEvidenceSnapshot evidence = mock(ReviewEvidenceSnapshot.class);
        when(evidencePort.findById(7L)).thenReturn(Optional.of(evidence));
        when(evidence.getImageUrls()).thenReturn(List.of(
                "/api/v1/media/images/11111111-1111-1111-1111-111111111111.jpg"));
        ReviewEvidenceMediaService service = new ReviewEvidenceMediaService(evidencePort, media);

        assertThatThrownBy(() -> service.getImage(7L, 1))
                .isInstanceOf(NotFoundException.class);
        verify(media, never()).get(anyString());
    }

    @Test
    @DisplayName("외부 URL인 증거 이미지는 로컬 저장소 조회 경로로 우회하지 않는다")
    void rejectExternalEvidenceImageUrl() {
        ReviewEvidencePort evidencePort = mock(ReviewEvidencePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ReviewEvidenceSnapshot evidence = mock(ReviewEvidenceSnapshot.class);
        when(evidencePort.findById(7L)).thenReturn(Optional.of(evidence));
        when(evidence.getImageUrls()).thenReturn(List.of("https://cdn.example/evidence.jpg"));
        ReviewEvidenceMediaService service = new ReviewEvidenceMediaService(evidencePort, media);

        assertThatThrownBy(() -> service.getImage(7L, 0))
                .isInstanceOf(NotFoundException.class);
        verify(media, never()).get(anyString());
    }
}
