package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.StoredImage;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultImageMediaServiceTest {

    private final ImageMediaStoragePort storage = mock(ImageMediaStoragePort.class);
    private final DefaultImageMediaService service = new DefaultImageMediaService(storage);

    @ParameterizedTest(name = "{0}")
    @MethodSource("supportedImages")
    @DisplayName("실제 JPEG, PNG, WebP 파일을 저장하고 공개 경로로 읽는다")
    void uploadAndReadImage(
            String imageType,
            byte[] image,
            String contentType,
            String extension
    ) {
        StoredImage stored = service.upload(image, contentType);
        verify(storage).store(eq(stored.fileName()), aryEq(image));
        when(storage.read(stored.fileName())).thenReturn(Optional.of(image));

        ImageContent loaded = service.get(stored.fileName());

        assertThat(stored.fileName()).as(imageType).endsWith("." + extension);
        assertThat(stored.url()).isEqualTo("/api/v1/media/images/" + stored.fileName());
        assertThat(loaded.contentType()).isEqualTo(contentType);
        assertThat(loaded.bytes()).containsExactly(image);
    }

    @Test
    @DisplayName("이미지 MIME 타입만 위조한 파일은 저장하지 않는다")
    void rejectSpoofedImage() {
        assertThatThrownBy(() -> service.upload("not-an-image".getBytes(), "image/png"))
                .isInstanceOf(HappyGalleryException.class);
        verify(storage, never()).store(anyString(), any(byte[].class));
    }

    @Test
    @DisplayName("실제 파일 형식과 선언한 MIME 타입이 다르면 저장하지 않는다")
    void rejectMismatchedContentType() {
        byte[] png = {
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
        };

        assertThatThrownBy(() -> service.upload(png, "image/jpeg"))
                .isInstanceOf(HappyGalleryException.class);
        verify(storage, never()).store(anyString(), any(byte[].class));
    }

    private static Stream<Arguments> supportedImages() {
        return Stream.of(
                Arguments.of(
                        "JPEG",
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
                        "image/jpeg",
                        "jpg"),
                Arguments.of(
                        "PNG",
                        new byte[]{
                                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                                0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
                        },
                        "image/png",
                        "png"),
                Arguments.of(
                        "WebP",
                        new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'},
                        "image/webp",
                        "webp"));
    }
}
