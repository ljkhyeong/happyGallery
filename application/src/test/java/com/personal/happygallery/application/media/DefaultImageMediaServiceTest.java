package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.StoredImage;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultImageMediaServiceTest {

    private final InMemoryStorage storage = new InMemoryStorage();
    private final DefaultImageMediaService service = new DefaultImageMediaService(storage);

    @Test
    @DisplayName("실제 PNG 서명을 가진 이미지만 저장하고 공개 경로로 읽는다")
    void uploadAndReadImage() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1};

        StoredImage stored = service.upload(png, "image/png");
        ImageContent loaded = service.get(stored.fileName());

        assertThat(stored.url()).isEqualTo("/api/v1/media/images/" + stored.fileName());
        assertThat(loaded.contentType()).isEqualTo("image/png");
        assertThat(loaded.bytes()).containsExactly(png);
    }

    @Test
    @DisplayName("이미지 MIME 타입만 위조한 파일은 저장하지 않는다")
    void rejectSpoofedImage() {
        assertThatThrownBy(() -> service.upload("not-an-image".getBytes(), "image/png"))
                .isInstanceOf(HappyGalleryException.class);
        assertThat(storage.files).isEmpty();
    }

    private static class InMemoryStorage implements ImageMediaStoragePort {
        private final Map<String, byte[]> files = new HashMap<>();

        @Override
        public void store(String fileName, byte[] bytes) {
            files.put(fileName, bytes);
        }

        @Override
        public Optional<byte[]> read(String fileName) {
            return Optional.ofNullable(files.get(fileName));
        }
    }
}
