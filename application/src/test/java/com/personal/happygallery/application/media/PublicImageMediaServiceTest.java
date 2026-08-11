package com.personal.happygallery.application.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort.ReferenceVisibility;
import com.personal.happygallery.domain.error.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PublicImageMediaServiceTest {

    private static final String FILE_NAME =
            "11111111-1111-1111-1111-111111111111.jpg";
    private static final String IMAGE_URL = "/api/v1/media/images/" + FILE_NAME;

    @Test
    @DisplayName("증거 또는 숨김 후기에만 남은 내부 전용 이미지는 인증 없는 공개 조회에서 찾을 수 없다")
    void rejectInternallyReferencedImage() {
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ImageMediaReferenceReaderPort references = mock(ImageMediaReferenceReaderPort.class);
        when(media.get(FILE_NAME)).thenReturn(image());
        when(references.findReferenceVisibility(IMAGE_URL))
                .thenReturn(new ReferenceVisibility(false, true));
        PublicImageMediaService service = new PublicImageMediaService(media, references);

        assertThatThrownBy(() -> service.get(FILE_NAME))
                .isInstanceOf(NotFoundException.class);
        verify(media, never()).get(FILE_NAME);
    }

    @Test
    @DisplayName("내부 참조와 현재 공개 엔티티가 함께 참조하는 로컬 이미지는 공개 조회를 유지한다")
    void allowInternallyReferencedImageStillPubliclyReferenced() {
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ImageMediaReferenceReaderPort references = mock(ImageMediaReferenceReaderPort.class);
        when(media.get(FILE_NAME)).thenReturn(image());
        when(references.findReferenceVisibility(IMAGE_URL))
                .thenReturn(new ReferenceVisibility(true, true));
        PublicImageMediaService service = new PublicImageMediaService(media, references);

        assertThat(service.get(FILE_NAME).bytes()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("아직 어떤 DB 참조도 없는 관리자 업로드 미리보기는 공개 조회를 유지한다")
    void allowFreshOrphanAdminPreview() {
        ImageMediaUseCase media = mock(ImageMediaUseCase.class);
        ImageMediaReferenceReaderPort references = mock(ImageMediaReferenceReaderPort.class);
        when(media.get(FILE_NAME)).thenReturn(image());
        when(references.findReferenceVisibility(IMAGE_URL))
                .thenReturn(new ReferenceVisibility(false, false));
        PublicImageMediaService service = new PublicImageMediaService(media, references);

        assertThat(service.get(FILE_NAME).contentType()).isEqualTo("image/jpeg");
        verify(references).findReferenceVisibility(IMAGE_URL);
    }

    private static ImageMediaUseCase.ImageContent image() {
        return new ImageMediaUseCase.ImageContent(new byte[] {1, 2, 3}, "image/jpeg");
    }
}
