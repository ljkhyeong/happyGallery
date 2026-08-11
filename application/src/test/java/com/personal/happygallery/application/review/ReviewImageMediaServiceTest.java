package com.personal.happygallery.application.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.application.review.port.out.ReviewListView;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewImage;
import com.personal.happygallery.domain.review.ReviewStatus;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewImageMediaServiceTest {

    @Test
    @DisplayName("회원 보호 경로는 소유한 활성 후기의 로컬 이미지만 반환한다")
    void readOwnedActiveReviewImage() {
        ReviewReaderPort reviewReader = mock(ReviewReaderPort.class);
        ReviewImagePort imagePort = mock(ReviewImagePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        Review review = mock(Review.class);
        ReviewImage reviewImage = mock(ReviewImage.class);
        String fileName = "11111111-1111-1111-1111-111111111111.png";
        when(reviewReader.findByIdAndUserId(7L, 3L)).thenReturn(Optional.of(review));
        when(review.getStatus()).thenReturn(ReviewStatus.HIDDEN);
        when(imagePort.findByIdAndReviewId(11L, 7L)).thenReturn(Optional.of(reviewImage));
        when(reviewImage.getImageUrl()).thenReturn("/api/v1/media/images/" + fileName);
        when(media.get(fileName)).thenReturn(new ImageMediaUseCase.ImageContent(
                new byte[] {1, 2, 3}, "image/png"));
        ReviewImageMediaService service = new ReviewImageMediaService(reviewReader, imagePort, media);

        var image = service.getOwnedImage(3L, 7L, 11L);

        assertThat(image.bytes()).containsExactly(1, 2, 3);
        assertThat(image.contentType()).isEqualTo("image/png");
        verify(review).requireActive();
        verify(media).get(fileName);
    }

    @Test
    @DisplayName("회원 보호 경로는 타인 후기에서 저장소 파일을 조회하지 않는다")
    void rejectReviewNotOwnedByMember() {
        ReviewReaderPort reviewReader = mock(ReviewReaderPort.class);
        ReviewImagePort imagePort = mock(ReviewImagePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        when(reviewReader.findByIdAndUserId(7L, 3L)).thenReturn(Optional.empty());
        ReviewImageMediaService service = new ReviewImageMediaService(reviewReader, imagePort, media);

        assertThatThrownBy(() -> service.getOwnedImage(3L, 7L, 11L))
                .isInstanceOf(NotFoundException.class);
        verify(imagePort, never()).findByIdAndReviewId(11L, 7L);
        verify(media, never()).get(anyString());
    }

    @Test
    @DisplayName("보호 경로는 공개 후기 이미지를 공개 미디어 경로와 중복 제공하지 않는다")
    void rejectPublishedReviewImage() {
        ReviewReaderPort reviewReader = mock(ReviewReaderPort.class);
        ReviewImagePort imagePort = mock(ReviewImagePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        Review ownedReview = mock(Review.class);
        ReviewListView adminReview = mock(ReviewListView.class);
        when(reviewReader.findByIdAndUserId(7L, 3L)).thenReturn(Optional.of(ownedReview));
        when(ownedReview.getStatus()).thenReturn(ReviewStatus.PUBLISHED);
        when(reviewReader.findViewById(7L)).thenReturn(Optional.of(adminReview));
        when(adminReview.status()).thenReturn(ReviewStatus.PUBLISHED);
        ReviewImageMediaService service = new ReviewImageMediaService(reviewReader, imagePort, media);

        assertThatThrownBy(() -> service.getOwnedImage(3L, 7L, 11L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.getAdminImage(7L, 11L))
                .isInstanceOf(NotFoundException.class);
        verify(imagePort, never()).findByIdAndReviewId(11L, 7L);
        verify(media, never()).get(anyString());
    }

    @Test
    @DisplayName("관리자 보호 경로는 현재 후기와 연결된 로컬 이미지만 반환한다")
    void readCurrentReviewImageForAdmin() {
        ReviewReaderPort reviewReader = mock(ReviewReaderPort.class);
        ReviewImagePort imagePort = mock(ReviewImagePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ReviewListView review = mock(ReviewListView.class);
        ReviewImage reviewImage = mock(ReviewImage.class);
        String fileName = "22222222-2222-2222-2222-222222222222.jpg";
        when(reviewReader.findViewById(7L)).thenReturn(Optional.of(review));
        when(review.status()).thenReturn(ReviewStatus.HIDDEN);
        when(imagePort.findByIdAndReviewId(11L, 7L)).thenReturn(Optional.of(reviewImage));
        when(reviewImage.getImageUrl()).thenReturn("/api/v1/media/images/" + fileName);
        when(media.get(fileName)).thenReturn(new ImageMediaUseCase.ImageContent(
                new byte[] {4, 5, 6}, "image/jpeg"));
        ReviewImageMediaService service = new ReviewImageMediaService(reviewReader, imagePort, media);

        var image = service.getAdminImage(7L, 11L);

        assertThat(image.bytes()).containsExactly(4, 5, 6);
        assertThat(image.contentType()).isEqualTo("image/jpeg");
        verify(media).get(fileName);
    }

    @Test
    @DisplayName("다른 후기에 연결된 이미지와 외부 URL은 보호 경로에서 파일을 읽지 않는다")
    void rejectImageOutsideReviewOrLocalStorage() {
        ReviewReaderPort reviewReader = mock(ReviewReaderPort.class);
        ReviewImagePort imagePort = mock(ReviewImagePort.class);
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ReviewListView review = mock(ReviewListView.class);
        ReviewImage externalImage = mock(ReviewImage.class);
        when(reviewReader.findViewById(7L)).thenReturn(Optional.of(review));
        when(review.status()).thenReturn(ReviewStatus.HIDDEN);
        when(imagePort.findByIdAndReviewId(11L, 7L)).thenReturn(Optional.empty());
        when(imagePort.findByIdAndReviewId(12L, 7L)).thenReturn(Optional.of(externalImage));
        when(externalImage.getImageUrl()).thenReturn("https://cdn.example/review.jpg");
        ReviewImageMediaService service = new ReviewImageMediaService(reviewReader, imagePort, media);

        assertThatThrownBy(() -> service.getAdminImage(7L, 11L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.getAdminImage(7L, 12L))
                .isInstanceOf(NotFoundException.class);
        verify(media, never()).get(anyString());
    }
}
