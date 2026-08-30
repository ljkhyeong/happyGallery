package com.personal.happygallery.application.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.time.Clocks;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicImageMediaServiceTest {

    private static final String FILE_NAME =
            "11111111-1111-1111-1111-111111111111.jpg";
    private static final String IMAGE_URL = "/api/v1/media/images/" + FILE_NAME;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T12:00:00Z"), Clocks.SEOUL);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Test
    @DisplayName("현재 공개 aggregate가 참조하지 않는 이미지는 저장소를 읽지 않고 숨긴다")
    void rejectImageWithoutPublicReference() {
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ImageMediaReferenceReaderPort references = mock(ImageMediaReferenceReaderPort.class);
        when(references.isPubliclyReferenced(IMAGE_URL, NOW)).thenReturn(false);
        PublicImageMediaService service = new PublicImageMediaService(media, references, CLOCK);

        assertThatThrownBy(() -> service.get(FILE_NAME))
                .isInstanceOf(NotFoundException.class);
        verify(references).isPubliclyReferenced(IMAGE_URL, NOW);
        verify(media, never()).get(FILE_NAME);
    }

    @Test
    @DisplayName("현재 공개 aggregate가 참조하는 이미지는 공개 조회한다")
    void servePubliclyReferencedImage() {
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ImageMediaReferenceReaderPort references = mock(ImageMediaReferenceReaderPort.class);
        when(media.get(FILE_NAME)).thenReturn(image());
        when(references.isPubliclyReferenced(IMAGE_URL, NOW)).thenReturn(true);
        PublicImageMediaService service = new PublicImageMediaService(media, references, CLOCK);

        assertThat(service.get(FILE_NAME).bytes()).containsExactly(1, 2, 3);
    }

    private static ImageMediaUseCase.ImageContent image() {
        return new ImageMediaUseCase.ImageContent(new byte[] {1, 2, 3}, "image/jpeg");
    }
}
